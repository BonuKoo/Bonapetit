package com.eatmate.post.service;

import com.eatmate.chat.redisDao.ChatCacheRepository;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.chat.service.ChatRoomService;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chat.ChatMessageRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.dao.repository.team.CustomTeamRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

/**
 * 모임 삭제.
 *
 * <p>Team은 cascade = ALL로 AccountTeam과 ChatRoom을 함께 지우지만 ChatRoom에는
 * 메시지 컬렉션이 없어 cascade가 chat_message까지 닿지 않는다. 그래서 <b>대화가 한 번이라도
 * 오간 모임은 삭제되지 않았다.</b>
 *
 * <pre>
 * ConstraintViolationException: CHAT_MESSAGE FOREIGN KEY(ROOM_ID) REFERENCES CHAT_ROOM(ROOM_ID)
 * </pre>
 */
@DataJpaTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class PostJpaServiceDeleteTeamTest {

    private static final String ROOM_ID = "room-doomed";
    private static final String LEADER_OAUTH_ID = "oauth-leader";
    private static final String MEMBER_OAUTH_ID = "oauth-member";

    @Autowired private AccountRepository accountRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private TestEntityManager em;

    @Mock private CustomTeamRepository customTeamRepository;
    @Mock private ChatCacheRepository chatCacheRepository;
    @Mock private ChatRoomRedisRepository chatRoomRedisRepository;
    @Mock private ChatRoomService chatRoomService;

    private PostJpaService postJpaService;
    private Long teamId;

    @BeforeEach
    void setUp() {
        postJpaService = new PostJpaService(accountRepository, teamRepository, customTeamRepository,
                chatMessageRepository, chatCacheRepository, chatRoomRedisRepository, chatRoomService);

        Team team = Team.builder().teamName("사라질 모임").build();
        ChatRoom room = ChatRoom.builder().roomId(ROOM_ID).roomName("사라질 모임").build();
        team.setChatRoom(room);
        room.setTeam(team);
        team.addAccountTeam(AccountTeam.builder().account(account("leader@x.com", "개설자", LEADER_OAUTH_ID)).isLeader(true).build());
        team.addAccountTeam(AccountTeam.builder().account(account("member@x.com", "참여자", MEMBER_OAUTH_ID)).isLeader(false).build());
        em.persist(team);
        em.persist(room);

        for (int i = 1; i <= 3; i++) {
            em.persist(ChatMessage.builder()
                    .chatRoom(room).senderName("개설자")
                    .type(ChatMessage.MessageType.TALK).message("메시지 " + i).build());
        }

        em.flush();
        teamId = team.getId();
        em.clear();
    }

    private Account account(String email, String nickname, String oauth2Id) {
        Account account = em.persist(Account.builder()
                .email(email).nickname(nickname).password("x").build());
        try {
            var f = Account.class.getDeclaredField("oauth2id");
            f.setAccessible(true);
            f.set(account, oauth2Id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return account;
    }

    @Test
    @DisplayName("대화가 오간 모임도 삭제할 수 있다")
    void teamWithMessagesCanBeDeleted() {
        assertThatCode(() -> {
            postJpaService.deleteTeam(teamId);
            em.flush();
        }).doesNotThrowAnyException();

        em.clear();
        assertThat(teamRepository.findById(teamId)).isEmpty();
        assertThat(chatRoomRepository.findById(ROOM_ID)).isEmpty();
        assertThat(chatMessageRepository.findLatestByRoomId(ROOM_ID, PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    @DisplayName("삭제 시 방 캐시와 참여자 전원의 멤버십 캐시를 무효화한다")
    void deletingTeamEvictsChatCaches() {
        postJpaService.deleteTeam(teamId);
        em.flush();

        // 멤버십 캐시가 살아 있으면 인가를 통과해 버리고, 방 조회를 건너뛰므로
        // 사라진 모임의 대화가 TTL(5분) 동안 계속 읽힌다.
        verify(chatCacheRepository).evictMember(ROOM_ID, LEADER_OAUTH_ID);
        verify(chatCacheRepository).evictMember(ROOM_ID, MEMBER_OAUTH_ID);
        verify(chatCacheRepository).evictRecent(ROOM_ID);
        // TTL이 없는 해시라 지우지 않으면 사라진 방이 목록에 영원히 남는다.
        verify(chatRoomRedisRepository).deleteChatRoom(ROOM_ID);
    }
}
