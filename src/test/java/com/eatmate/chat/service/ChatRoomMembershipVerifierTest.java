package com.eatmate.chat.service;

import com.eatmate.chat.redisDao.ChatCacheRepository;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 채팅방 멤버십 검증.
 *
 * 원래 ChatHistoryServiceTest에 있던 인가 절을 옮겨 왔다. 검증이 HTTP 내역 조회와
 * STOMP 구독 양쪽에서 쓰이게 되면서, 검증의 계약을 한 곳에서 고정해 둔다.
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomMembershipVerifierTest {

    private static final String ROOM_ID = "room-1";
    private static final String OAUTH_ID = "oauth-1";

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountTeamRepository accountTeamRepository;
    @Mock private ChatCacheRepository chatCacheRepository;

    @InjectMocks private ChatRoomMembershipVerifier verifier;

    private Team team;
    private Account account;

    @BeforeEach
    void setUp() {
        team = Team.builder().id(1L).teamName("A팀").build();
        account = Account.builder().email("a@b.com").nickname("테스터").password("x").build();
    }

    private ChatRoom room(Team boundTeam) {
        return ChatRoom.builder().roomId(ROOM_ID).roomName("A팀 채팅방").team(boundTeam).build();
    }

    @Test
    @DisplayName("팀 멤버는 통과한다")
    void memberPasses() {
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room(team)));
        given(accountRepository.findByOauth2id(OAUTH_ID)).willReturn(Optional.of(account));
        given(accountTeamRepository.findByAccountAndTeam(account, team))
                .willReturn(Optional.of(AccountTeam.builder().account(account).team(team).build()));

        assertThatCode(() -> verifier.verify(ROOM_ID, OAUTH_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("팀 멤버가 아니면 거부한다")
    void nonMemberIsDenied() {
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room(team)));
        given(accountRepository.findByOauth2id(OAUTH_ID)).willReturn(Optional.of(account));
        given(accountTeamRepository.findByAccountAndTeam(account, team)).willReturn(Optional.empty());

        assertThatThrownBy(() -> verifier.verify(ROOM_ID, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 방은 404다")
    void unknownRoomIsNotFound() {
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> verifier.verify(ROOM_ID, OAUTH_ID))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("팀이 연결되지 않은 방은 아무도 볼 수 없다")
    void roomWithoutTeamIsDenied() {
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room(null)));

        assertThatThrownBy(() -> verifier.verify(ROOM_ID, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("계정을 찾을 수 없으면 거부한다")
    void unknownAccountIsDenied() {
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room(team)));
        given(accountRepository.findByOauth2id(OAUTH_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> verifier.verify(ROOM_ID, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("인증 주체가 없으면 DB를 보지 않고 거부한다")
    void missingPrincipalIsDenied() {
        // STOMP 구독은 인증 객체가 비어 있는 채로 들어올 수 있다. 그 경우 방을 조회할
        // 것도 없이 막아야 한다.
        assertThatThrownBy(() -> verifier.verify(ROOM_ID, null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(chatRoomRepository, accountRepository, accountTeamRepository);
    }

    @Test
    @DisplayName("멤버십이 캐시돼 있으면 인가 쿼리를 하지 않는다")
    void membershipCacheHitSkipsAuthQueries() {
        given(chatCacheRepository.isMemberVerified(ROOM_ID, OAUTH_ID)).willReturn(true);

        verifier.verify(ROOM_ID, OAUTH_ID);

        // 방 조회 · 계정 조회 · 멤버십 조회 3건이 모두 생략되어야 한다.
        verifyNoInteractions(chatRoomRepository, accountRepository, accountTeamRepository);
    }

    @Test
    @DisplayName("검증을 통과하면 캐시에 기록한다")
    void membershipIsCachedAfterVerification() {
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room(team)));
        given(accountRepository.findByOauth2id(OAUTH_ID)).willReturn(Optional.of(account));
        given(accountTeamRepository.findByAccountAndTeam(account, team))
                .willReturn(Optional.of(AccountTeam.builder().account(account).team(team).build()));

        verifier.verify(ROOM_ID, OAUTH_ID);

        verify(chatCacheRepository).markMemberVerified(ROOM_ID, OAUTH_ID);
    }

    @Test
    @DisplayName("멤버가 아니면 멤버십을 캐시하지 않는다")
    void nonMemberIsNotCached() {
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room(team)));
        given(accountRepository.findByOauth2id(OAUTH_ID)).willReturn(Optional.of(account));
        given(accountTeamRepository.findByAccountAndTeam(account, team)).willReturn(Optional.empty());

        assertThatThrownBy(() -> verifier.verify(ROOM_ID, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);

        // 비멤버까지 캐싱하면 방금 참여한 사용자가 TTL 동안 차단된다.
        verify(chatCacheRepository, never()).markMemberVerified(anyString(), anyString());
    }
}
