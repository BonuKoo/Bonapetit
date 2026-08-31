package com.eatmate.chat.redisDao;

import com.eatmate.chat.dto.ChatMessageResponse;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 캐시의 핵심 계약은 "Redis가 죽어도 서비스는 살아있다"이다.
 *
 * 메시지 영속화는 정합성을 우선해 저장 실패 시 발행까지 막지만,
 * 캐시는 가용성이 우선이라 어떤 Redis 오류도 밖으로 새어 나가면 안 된다.
 * 예외가 전파되면 Redis 장애가 곧 채팅 조회 장애가 된다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatCacheRepositoryTest {

    private static final String ROOM_ID = "room-1";
    private static final String OAUTH_ID = "oauth-1";

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ListOperations<String, String> listOps;
    @Mock private ValueOperations<String, String> valueOps;

    private ChatCacheRepository chatCacheRepository;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        chatCacheRepository = new ChatCacheRepository(stringRedisTemplate, objectMapper);

        given(stringRedisTemplate.opsForList()).willReturn(listOps);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
    }

    private ChatMessageResponse message(long id) {
        return new ChatMessageResponse(id, "테스터", "메시지 " + id,
                ChatMessage.MessageType.TALK, LocalDateTime.of(2026, 8, 31, 12, 0));
    }

    // ────────────────── 정상 동작 ──────────────────

    @Test
    @DisplayName("저장한 메시지를 그대로 읽어온다")
    void roundTripsMessages() {
        // warmRecent가 직렬화한 것을 그대로 돌려주는 상황
        given(listOps.range(anyString(), eq(0L), eq(1L)))
                .willAnswer(inv -> List.of(
                        JsonMapper.builder().addModule(new JavaTimeModule()).build()
                                .writeValueAsString(message(2))));

        Optional<List<ChatMessageResponse>> result = chatCacheRepository.getRecent(ROOM_ID, 2);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        assertThat(result.get().get(0).id()).isEqualTo(2L);
        assertThat(result.get().get(0).message()).isEqualTo("메시지 2");
    }

    @Test
    @DisplayName("캐시가 비어 있으면 미스로 처리한다")
    void emptyCacheIsMiss() {
        given(listOps.range(anyString(), anyLong(), anyLong())).willReturn(List.of());

        assertThat(chatCacheRepository.getRecent(ROOM_ID, 10)).isEmpty();
    }

    @Test
    @DisplayName("캐시 적재는 기존 내용을 지우고 통째로 교체한다")
    void warmReplacesInsteadOfAppending() {
        chatCacheRepository.warmRecent(ROOM_ID, List.of(message(2), message(1)));

        // 부분 갱신이면 이전 내용이 남아 순서와 개수가 어긋난다.
        InOrder order = inOrder(stringRedisTemplate, listOps);
        order.verify(stringRedisTemplate).delete(anyString());
        order.verify(listOps).rightPushAll(anyString(), anyList());
    }

    @Test
    @DisplayName("키가 없으면 push하지 않는다")
    void pushIsSkippedWhenKeyAbsent() {
        // LPUSHX는 키가 없으면 0을 반환한다.
        given(listOps.leftPushIfPresent(anyString(), anyString())).willReturn(0L);

        chatCacheRepository.pushRecent(ROOM_ID, message(1));

        // 반쪽짜리 캐시를 만들지 않기 위해 trim도 하지 않아야 한다.
        verify(listOps, never()).trim(anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("push 후 용량을 넘지 않도록 잘라낸다")
    void pushTrimsToCapacity() {
        given(listOps.leftPushIfPresent(anyString(), anyString())).willReturn(150L);

        chatCacheRepository.pushRecent(ROOM_ID, message(1));

        verify(listOps).trim(anyString(), eq(0L), eq((long) ChatCacheRepository.RECENT_CAPACITY - 1));
    }

    // ────────────────── 실패 격리 ──────────────────

    @Test
    @DisplayName("조회 중 Redis가 죽어도 예외를 던지지 않고 미스로 처리한다")
    void readFailureFallsBackToMiss() {
        given(listOps.range(anyString(), anyLong(), anyLong()))
                .willThrow(new RedisConnectionFailureException("연결 불가"));

        Optional<List<ChatMessageResponse>> result = chatCacheRepository.getRecent(ROOM_ID, 10);

        // 미스로 처리되어야 호출부가 DB로 폴백한다.
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("적재 중 Redis가 죽어도 예외를 던지지 않는다")
    void warmFailureIsSwallowed() {
        given(stringRedisTemplate.delete(anyString()))
                .willThrow(new RedisConnectionFailureException("연결 불가"));

        assertThatCode(() -> chatCacheRepository.warmRecent(ROOM_ID, List.of(message(1))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("갱신에 실패하면 낡은 캐시를 남기지 않고 폐기한다")
    void pushFailureEvictsStaleCache() {
        given(listOps.leftPushIfPresent(anyString(), anyString()))
                .willThrow(new RedisConnectionFailureException("연결 불가"));

        assertThatCode(() -> chatCacheRepository.pushRecent(ROOM_ID, message(1)))
                .doesNotThrowAnyException();

        // 갱신이 밀리면 낡은 첫 페이지를 보여주게 되므로 아예 버려야 한다.
        verify(stringRedisTemplate).delete("CHAT_RECENT:" + ROOM_ID);
    }

    @Test
    @DisplayName("멤버십 조회가 실패하면 미확인으로 처리해 DB 검증을 유도한다")
    void membershipReadFailureReturnsFalse() {
        given(stringRedisTemplate.hasKey(anyString()))
                .willThrow(new RedisConnectionFailureException("연결 불가"));

        // true를 반환하면 Redis 장애가 인가 우회로 이어진다. 반드시 false여야 한다.
        assertThat(chatCacheRepository.isMemberVerified(ROOM_ID, OAUTH_ID)).isFalse();
    }

    @Test
    @DisplayName("멤버십 기록·삭제가 실패해도 예외를 던지지 않는다")
    void membershipWriteFailureIsSwallowed() {
        doThrow(new RedisConnectionFailureException("연결 불가"))
                .when(valueOps).set(anyString(), anyString(), any(java.time.Duration.class));
        given(stringRedisTemplate.delete(anyString()))
                .willThrow(new RedisConnectionFailureException("연결 불가"));

        assertThatCode(() -> chatCacheRepository.markMemberVerified(ROOM_ID, OAUTH_ID))
                .doesNotThrowAnyException();
        assertThatCode(() -> chatCacheRepository.evictMember(ROOM_ID, OAUTH_ID))
                .doesNotThrowAnyException();
    }
}
