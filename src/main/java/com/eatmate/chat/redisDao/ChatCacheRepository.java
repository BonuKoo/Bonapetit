package com.eatmate.chat.redisDao;

import com.eatmate.chat.dto.ChatMessageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 채팅방 진입 속도를 위한 Redis 캐시.
 *
 * 두 가지를 캐싱한다.
 *   1) 방별 최신 메시지 목록 (List)  — 진입 시 첫 페이지
 *   2) 멤버십 검증 결과 (String)     — 진입 시 인가 쿼리 3건
 *
 * 캐시가 없던 시절 방 진입은 DB 쿼리 4건이었고, 그중 3건이 인가 검증이었다.
 *
 * <h3>실패 정책</h3>
 * 모든 Redis 접근은 예외를 삼키고 "캐시 미스" 또는 "무동작"으로 처리한다.
 * 메시지 영속화는 정합성을 우선해 실패 시 발행까지 막지만, <b>캐시는 가용성이 우선</b>이다.
 * Redis가 죽어도 채팅 조회는 DB로 폴백되어 정상 동작해야 한다.
 *
 * <h3>일관성</h3>
 * 최신 목록은 write-through로 유지한다. 다만 갱신에 LPUSHX를 쓰기 때문에
 * <b>키가 이미 존재할 때만</b> 밀어 넣는다. 키가 없는 상태에서 push하면 최신 몇 건만 든
 * 반쪽짜리 캐시가 만들어져, 그 뒤 조회가 "더 이전 메시지가 없다"고 잘못 판단하게 된다.
 * 캐시는 반드시 {@link #warmRecent}로 통째로 채워야 하며, 그래야
 * "캐시 길이 &lt; 최대치"가 곧 "실제로 그만큼뿐"을 의미해 hasMore 판정이 정확해진다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatCacheRepository {

    /**
     * 방마다 보관할 최신 메시지 수.
     *
     * 조회 size 상한이 100인데 101로 잡은 이유는, 다음 페이지 존재 여부를 판정하려면
     * 보여줄 개수보다 1건을 더 봐야 하기 때문이다. 100으로 두면 size=100 요청만
     * 캐시를 못 타는 어정쩡한 구멍이 생긴다.
     */
    public static final int RECENT_CAPACITY = 101;

    private static final String RECENT_KEY_PREFIX = "CHAT_RECENT:";
    private static final String AUTH_KEY_PREFIX = "CHAT_AUTH:";

    /** 최신 목록 TTL. 오래 안 쓰는 방의 캐시가 무한정 쌓이지 않도록 접근 시마다 갱신한다. */
    private static final Duration RECENT_TTL = Duration.ofDays(1);

    /**
     * 멤버십 캐시 TTL. 짧게 두는 이유는 강퇴·탈퇴가 권한을 <b>빼앗는</b> 작업이기 때문이다.
     * 명시적 무효화({@link #evictMember})를 걸어 두었지만, 놓치는 경로가 생기더라도
     * 이 시간 안에 스스로 회복되도록 한다.
     */
    private static final Duration AUTH_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private String recentKey(String roomId) {
        return RECENT_KEY_PREFIX + roomId;
    }

    private String authKey(String roomId, String oauth2Id) {
        return AUTH_KEY_PREFIX + roomId + ":" + oauth2Id;
    }

    // ────────────────────────────── 최신 메시지 ──────────────────────────────

    /**
     * 최신 메시지를 앞에서부터 count건 읽는다.
     *
     * @return 캐시가 비었거나 Redis 접근에 실패하면 {@link Optional#empty()}.
     *         빈 리스트와 캐시 미스를 구분해야 호출부가 DB 폴백 여부를 판단할 수 있다.
     */
    public Optional<List<ChatMessageResponse>> getRecent(String roomId, int count) {
        try {
            List<String> raw = stringRedisTemplate.opsForList()
                    .range(recentKey(roomId), 0, count - 1L);

            if (raw == null || raw.isEmpty()) {
                return Optional.empty();
            }

            List<ChatMessageResponse> messages = new ArrayList<>(raw.size());
            for (String json : raw) {
                messages.add(objectMapper.readValue(json, ChatMessageResponse.class));
            }
            // 접근이 있었으므로 만료를 미룬다.
            stringRedisTemplate.expire(recentKey(roomId), RECENT_TTL);
            return Optional.of(messages);

        } catch (Exception e) {
            log.warn("채팅 캐시 조회 실패 - DB로 폴백한다. roomId={}", roomId, e);
            return Optional.empty();
        }
    }

    /**
     * DB에서 읽은 최신 목록으로 캐시를 통째로 교체한다.
     *
     * @param messages 최신순(id 내림차순) 목록. 캐시도 같은 순서를 유지한다.
     */
    public void warmRecent(String roomId, List<ChatMessageResponse> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        try {
            List<String> payload = new ArrayList<>(messages.size());
            for (ChatMessageResponse m : messages) {
                payload.add(objectMapper.writeValueAsString(m));
            }

            String key = recentKey(roomId);
            // 부분 갱신이 아니라 교체다. 이전 내용이 남으면 순서와 개수가 어긋난다.
            stringRedisTemplate.delete(key);
            stringRedisTemplate.opsForList().rightPushAll(key, payload);
            stringRedisTemplate.expire(key, RECENT_TTL);

        } catch (Exception e) {
            log.warn("채팅 캐시 적재 실패 - 캐시 없이 진행한다. roomId={}", roomId, e);
        }
    }

    /**
     * 새 메시지를 캐시 앞에 밀어 넣는다.
     *
     * LPUSHX를 쓰므로 <b>키가 이미 있을 때만</b> 동작한다. 캐시가 없는 방은 그대로 두고,
     * 다음 조회에서 {@link #warmRecent}로 통째로 채워지게 한다.
     */
    public void pushRecent(String roomId, ChatMessageResponse message) {
        try {
            String key = recentKey(roomId);
            Long size = stringRedisTemplate.opsForList()
                    .leftPushIfPresent(key, objectMapper.writeValueAsString(message));

            if (size == null || size == 0) {
                return; // 캐시가 없는 방. 다음 조회에서 채워진다.
            }
            stringRedisTemplate.opsForList().trim(key, 0, RECENT_CAPACITY - 1L);
            stringRedisTemplate.expire(key, RECENT_TTL);

        } catch (Exception e) {
            // 캐시가 뒤처지면 낡은 첫 페이지를 보여주게 되므로, 실패 시 아예 버린다.
            log.warn("채팅 캐시 갱신 실패 - 해당 방 캐시를 폐기한다. roomId={}", roomId, e);
            evictRecent(roomId);
        }
    }

    public void evictRecent(String roomId) {
        try {
            stringRedisTemplate.delete(recentKey(roomId));
        } catch (Exception e) {
            log.warn("채팅 캐시 삭제 실패. roomId={}", roomId, e);
        }
    }

    // ────────────────────────────── 멤버십 ──────────────────────────────

    /**
     * 멤버로 확인된 적이 있는지. 미확인이거나 Redis 접근 실패면 false를 돌려
     * 호출부가 DB로 검증하게 한다.
     *
     * 멤버인 경우만 캐싱한다. 비멤버까지 캐싱하면 방금 참여한 사용자가
     * TTL이 끝날 때까지 차단되는 문제가 생긴다.
     */
    public boolean isMemberVerified(String roomId, String oauth2Id) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(authKey(roomId, oauth2Id)));
        } catch (Exception e) {
            log.warn("멤버십 캐시 조회 실패 - DB로 검증한다. roomId={}", roomId, e);
            return false;
        }
    }

    public void markMemberVerified(String roomId, String oauth2Id) {
        try {
            stringRedisTemplate.opsForValue().set(authKey(roomId, oauth2Id), "1", AUTH_TTL);
        } catch (Exception e) {
            log.warn("멤버십 캐시 적재 실패 - 캐시 없이 진행한다. roomId={}", roomId, e);
        }
    }

    /** 탈퇴·강퇴 시 호출한다. 권한을 빼앗는 작업이므로 즉시 반영되어야 한다. */
    public void evictMember(String roomId, String oauth2Id) {
        try {
            stringRedisTemplate.delete(authKey(roomId, oauth2Id));
        } catch (Exception e) {
            log.warn("멤버십 캐시 삭제 실패. roomId={} oauth2Id={}", roomId, oauth2Id, e);
        }
    }
}
