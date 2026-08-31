package com.eatmate.chat.service;

import com.eatmate.chat.dto.ChatHistoryResponse;
import com.eatmate.chat.dto.ChatMessageResponse;
import com.eatmate.chat.redisDao.ChatCacheRepository;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chat.ChatMessageRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    private static final String ROOM_ID = "room-1";
    private static final String OAUTH_ID = "oauth-1";

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountTeamRepository accountTeamRepository;
    @Mock private ChatCacheRepository chatCacheRepository;

    @InjectMocks private ChatHistoryService chatHistoryService;

    private Team team;
    private Account account;

    @BeforeEach
    void setUp() {
        team = Team.builder().id(1L).teamName("A팀").build();
        account = Account.builder().email("a@b.com").nickname("테스터").password("x").build();
    }

    /** 멤버십 검증을 통과시키는 기본 스텁 */
    private void givenMember() {
        ChatRoom room = ChatRoom.builder().roomId(ROOM_ID).roomName("A팀 채팅방").team(team).build();
        lenient().when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        lenient().when(accountRepository.findByOauth2id(OAUTH_ID)).thenReturn(Optional.of(account));
        lenient().when(accountTeamRepository.findByAccountAndTeam(account, team))
                .thenReturn(Optional.of(AccountTeam.builder().account(account).team(team).build()));
    }

    private List<ChatMessage> messages(int count) {
        List<ChatMessage> list = new ArrayList<>();
        // id 내림차순으로 반환되는 저장소를 흉내낸다.
        for (int i = count; i >= 1; i--) {
            list.add(messageWithId((long) i, "메시지 " + i));
        }
        return list;
    }

    private ChatMessage messageWithId(Long id, String text) {
        ChatMessage m = ChatMessage.builder()
                .senderName("테스터")
                .type(ChatMessage.MessageType.TALK)
                .message(text)
                .build();
        // id는 IDENTITY라 세터가 없다. 조회 결과를 흉내내야 하므로 리플렉션으로 주입한다.
        try {
            var f = ChatMessage.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(m, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return m;
    }

    @Test
    @DisplayName("응답은 오래된 것부터 최신 순으로 뒤집혀 내려간다")
    void messagesAreReversedToChronologicalOrder() {
        givenMember();
        given(chatMessageRepository.findLatestByRoomId(eq(ROOM_ID), any(Pageable.class)))
                .willReturn(messages(3));

        ChatHistoryResponse response = chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        assertThat(response.messages()).extracting("message")
                .containsExactly("메시지 1", "메시지 2", "메시지 3");
    }

    @Test
    @DisplayName("size보다 많이 조회되면 hasMore가 true이고 size만큼만 잘라낸다")
    void hasMoreIsTrueWhenExtraRowExists() {
        givenMember();
        // size=3 이면 서비스는 4건을 조회한다. 4건이 오면 다음 페이지가 있다는 뜻.
        given(chatMessageRepository.findLatestByRoomId(eq(ROOM_ID), any(Pageable.class)))
                .willReturn(messages(4));

        ChatHistoryResponse response = chatHistoryService.getHistory(ROOM_ID, null, 3, OAUTH_ID);

        assertThat(response.hasMore()).isTrue();
        assertThat(response.messages()).hasSize(3);
        // 최신 3건(4,3,2)을 시간순으로 뒤집은 결과
        assertThat(response.messages()).extracting("message")
                .containsExactly("메시지 2", "메시지 3", "메시지 4");
    }

    @Test
    @DisplayName("size와 같거나 적게 조회되면 hasMore가 false다")
    void hasMoreIsFalseOnLastPage() {
        givenMember();
        given(chatMessageRepository.findLatestByRoomId(eq(ROOM_ID), any(Pageable.class)))
                .willReturn(messages(2));

        ChatHistoryResponse response = chatHistoryService.getHistory(ROOM_ID, null, 3, OAUTH_ID);

        assertThat(response.hasMore()).isFalse();
        assertThat(response.messages()).hasSize(2);
    }

    @Test
    @DisplayName("nextCursor는 페이지에서 가장 오래된 메시지의 id다")
    void nextCursorIsOldestIdInPage() {
        givenMember();
        given(chatMessageRepository.findLatestByRoomId(eq(ROOM_ID), any(Pageable.class)))
                .willReturn(messages(3));

        ChatHistoryResponse response = chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        assertThat(response.nextCursor()).isEqualTo(1L);
    }

    @Test
    @DisplayName("결과가 없으면 nextCursor는 null이다")
    void nextCursorIsNullWhenEmpty() {
        givenMember();
        given(chatMessageRepository.findLatestByRoomId(eq(ROOM_ID), any(Pageable.class)))
                .willReturn(List.of());

        ChatHistoryResponse response = chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        assertThat(response.messages()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    @DisplayName("before가 있으면 커서 쿼리를 쓴다")
    void beforeCursorUsesCursorQuery() {
        givenMember();
        given(chatMessageRepository.findByRoomIdBefore(eq(ROOM_ID), eq(7L), any(Pageable.class)))
                .willReturn(messages(2));

        chatHistoryService.getHistory(ROOM_ID, 7L, 10, OAUTH_ID);

        // findLatest가 아니라 findBefore가 쓰여야 한다.
        org.mockito.Mockito.verify(chatMessageRepository)
                .findByRoomIdBefore(eq(ROOM_ID), eq(7L), any(Pageable.class));
        org.mockito.Mockito.verify(chatMessageRepository, org.mockito.Mockito.never())
                .findLatestByRoomId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("size는 상한으로 잘린다")
    void sizeIsClampedToMax() {
        givenMember();
        given(chatMessageRepository.findByRoomIdBefore(eq(ROOM_ID), eq(500L), any(Pageable.class)))
                .willReturn(List.of());

        // 스크롤 구간(before 있음)은 캐시를 타지 않으므로 클램프 결과가 그대로 쿼리에 반영된다.
        chatHistoryService.getHistory(ROOM_ID, 500L, 100_000, OAUTH_ID);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(chatMessageRepository)
                .findByRoomIdBefore(eq(ROOM_ID), eq(500L), captor.capture());
        // 상한 100 + 판정용 1건
        assertThat(captor.getValue().getPageSize()).isEqualTo(ChatHistoryService.MAX_SIZE + 1);
    }

    @Test
    @DisplayName("size가 없거나 0 이하면 기본값을 쓴다")
    void sizeFallsBackToDefault() {
        givenMember();
        given(chatMessageRepository.findByRoomIdBefore(eq(ROOM_ID), eq(500L), any(Pageable.class)))
                .willReturn(List.of());

        chatHistoryService.getHistory(ROOM_ID, 500L, null, OAUTH_ID);
        chatHistoryService.getHistory(ROOM_ID, 500L, 0, OAUTH_ID);
        chatHistoryService.getHistory(ROOM_ID, 500L, -5, OAUTH_ID);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(chatMessageRepository, org.mockito.Mockito.times(3))
                .findByRoomIdBefore(eq(ROOM_ID), eq(500L), captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(p ->
                assertThat(p.getPageSize()).isEqualTo(ChatHistoryService.DEFAULT_SIZE + 1));
    }

    @Test
    @DisplayName("팀 멤버가 아니면 조회할 수 없다")
    void nonMemberIsDenied() {
        ChatRoom room = ChatRoom.builder().roomId(ROOM_ID).roomName("A팀 채팅방").team(team).build();
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(accountRepository.findByOauth2id(OAUTH_ID)).willReturn(Optional.of(account));
        given(accountTeamRepository.findByAccountAndTeam(account, team)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);

        org.mockito.Mockito.verifyNoInteractions(chatMessageRepository);
    }

    @Test
    @DisplayName("존재하지 않는 방은 404다")
    void unknownRoomIsNotFound() {
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID))
                .isInstanceOf(ResponseStatusException.class);

        org.mockito.Mockito.verifyNoInteractions(chatMessageRepository);
    }

    @Test
    @DisplayName("계정을 찾을 수 없으면 조회할 수 없다")
    void unknownAccountIsDenied() {
        ChatRoom room = ChatRoom.builder().roomId(ROOM_ID).roomName("A팀 채팅방").team(team).build();
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(accountRepository.findByOauth2id(OAUTH_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);

        org.mockito.Mockito.verifyNoInteractions(chatMessageRepository);
    }

    // ────────────────────────────── 캐시 ──────────────────────────────

    private List<ChatMessageResponse> cachedMessages(int count) {
        List<ChatMessageResponse> list = new ArrayList<>();
        for (int i = count; i >= 1; i--) {
            list.add(new ChatMessageResponse((long) i, "테스터", "메시지 " + i,
                    ChatMessage.MessageType.TALK, LocalDateTime.of(2026, 8, 31, 12, 0)));
        }
        return list;
    }

    @Test
    @DisplayName("캐시가 있으면 메시지 DB를 조회하지 않는다")
    void cacheHitSkipsDatabase() {
        givenMember();
        given(chatCacheRepository.getRecent(ROOM_ID, 11)).willReturn(Optional.of(cachedMessages(3)));

        ChatHistoryResponse response = chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        assertThat(response.messages()).extracting("message")
                .containsExactly("메시지 1", "메시지 2", "메시지 3");
        org.mockito.Mockito.verifyNoInteractions(chatMessageRepository);
    }

    @Test
    @DisplayName("캐시가 없으면 용량만큼 읽어 캐시를 채운다")
    void cacheMissWarmsWithFullCapacity() {
        givenMember();
        given(chatCacheRepository.getRecent(eq(ROOM_ID), anyInt())).willReturn(Optional.empty());
        given(chatMessageRepository.findLatestByRoomId(eq(ROOM_ID), any(Pageable.class)))
                .willReturn(messages(3));

        chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        // 요청분(11건)이 아니라 용량(101건)만큼 읽어야 한다.
        // 요청분만 채우면 반쪽짜리 캐시가 되어 이후 hasMore 판정이 틀린다.
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(chatMessageRepository)
                .findLatestByRoomId(eq(ROOM_ID), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(ChatCacheRepository.RECENT_CAPACITY);

        org.mockito.Mockito.verify(chatCacheRepository)
                .warmRecent(eq(ROOM_ID), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("위로 스크롤하는 구간은 캐시를 쓰지 않는다")
    void scrollBackDoesNotUseCache() {
        givenMember();
        given(chatMessageRepository.findByRoomIdBefore(eq(ROOM_ID), eq(7L), any(Pageable.class)))
                .willReturn(messages(2));

        chatHistoryService.getHistory(ROOM_ID, 7L, 10, OAUTH_ID);

        // 캐시와 DB에 걸친 커서 계산을 피하기 위해, 첫 페이지만 캐시를 탄다.
        org.mockito.Mockito.verify(chatCacheRepository, org.mockito.Mockito.never())
                .getRecent(anyString(), anyInt());
    }

    @Test
    @DisplayName("멤버십이 캐시돼 있으면 인가 쿼리를 하지 않는다")
    void membershipCacheHitSkipsAuthQueries() {
        given(chatCacheRepository.isMemberVerified(ROOM_ID, OAUTH_ID)).willReturn(true);
        given(chatCacheRepository.getRecent(ROOM_ID, 11)).willReturn(Optional.of(cachedMessages(1)));

        chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        // 방 조회 · 계정 조회 · 멤버십 조회 3건이 모두 생략되어야 한다.
        org.mockito.Mockito.verifyNoInteractions(chatRoomRepository);
        org.mockito.Mockito.verifyNoInteractions(accountRepository);
        org.mockito.Mockito.verifyNoInteractions(accountTeamRepository);
    }

    @Test
    @DisplayName("멤버십 검증을 통과하면 캐시에 기록한다")
    void membershipIsCachedAfterVerification() {
        givenMember();
        given(chatCacheRepository.getRecent(ROOM_ID, 11)).willReturn(Optional.of(cachedMessages(1)));

        chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        org.mockito.Mockito.verify(chatCacheRepository).markMemberVerified(ROOM_ID, OAUTH_ID);
    }

    @Test
    @DisplayName("멤버가 아니면 멤버십을 캐시하지 않는다")
    void nonMemberIsNotCached() {
        ChatRoom room = ChatRoom.builder().roomId(ROOM_ID).roomName("A팀 채팅방").team(team).build();
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(accountRepository.findByOauth2id(OAUTH_ID)).willReturn(Optional.of(account));
        given(accountTeamRepository.findByAccountAndTeam(account, team)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);

        // 비멤버까지 캐싱하면 방금 참여한 사용자가 TTL 동안 차단된다.
        org.mockito.Mockito.verify(chatCacheRepository, org.mockito.Mockito.never())
                .markMemberVerified(anyString(), anyString());
    }
}
