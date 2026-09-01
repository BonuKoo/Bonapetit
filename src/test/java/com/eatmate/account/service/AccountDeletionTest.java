package com.eatmate.account.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chat.ChatMessageRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 회원 탈퇴가 대화를 남기고 계정만 지우는지 확인한다.
 *
 * <p>account를 참조하는 테이블이 셋(notice · account_team · chat_message)인데
 * chat_message 정리가 빠져 있었다. FK 제약에 걸려 <b>대화에 한 번이라도 참여한
 * 사용자는 탈퇴가 실패</b>했다. 메시지 영속화(PR #70)로 생긴 회귀다.
 *
 * <p>서비스를 직접 호출하는 이유는, 결함이 SQL이 아니라 <b>서비스가 한 단계를
 * 빠뜨린 것</b>이었기 때문이다. SQL만 검증하면 같은 실수를 다시 놓친다.
 * MyBatis와 JPA는 JpaTransactionManager가 EntityManagerFactory에서 끌어온
 * 같은 DataSource를 쓰므로 한 트랜잭션에 묶인다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountDeletionTest {

    private static final String OAUTH_ID = "oauth-leaving";
    private static final String ROOM_ID = "room-deletion";

    /** RedisMessageListenerContainer는 SmartLifecycle이라 기동 시 실제로 구독을 시도한다. */
    @MockBean private RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired private AccountMyBatisService accountMyBatisService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private TeamRepository teamRepository;

    @PersistenceContext(unitName = "persistence")
    private EntityManager em;

    private Account leaving;

    @BeforeEach
    void setUp() {
        leaving = accountRepository.save(Account.builder()
                .email("leaving@eatmate.com").nickname("떠나는 사람").password("x").build());
        setOauth2Id(leaving, OAUTH_ID);

        Team team = teamRepository.save(Team.builder().teamName("A팀").build());
        ChatRoom room = chatRoomRepository.save(
                ChatRoom.builder().roomId(ROOM_ID).roomName("A팀 채팅방").team(team).build());

        chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room).sender(leaving).senderName("떠나는 사람")
                .type(ChatMessage.MessageType.TALK).message("먼저 갈게요").build());

        // MyBatis는 JPA의 영속성 컨텍스트를 모른다. 커넥션에 반영해 둬야 보인다.
        em.flush();
        em.clear();
    }

    private void setOauth2Id(Account account, String value) {
        try {
            var f = Account.class.getDeclaredField("oauth2id");
            f.setAccessible(true);
            f.set(account, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("대화에 참여한 계정도 탈퇴할 수 있다")
    void accountWithMessagesCanBeDeleted() {
        assertThatCode(() -> accountMyBatisService
                .deleteUserByOauth2Id(OAUTH_ID, String.valueOf(leaving.getId())))
                .doesNotThrowAnyException();

        em.clear();
        assertThat(accountRepository.findByOauth2id(OAUTH_ID)).isEmpty();
    }

    @Test
    @DisplayName("탈퇴해도 대화는 남고, 표시 이름은 스냅샷으로 보존된다")
    void messagesSurviveWithSenderNameSnapshot() {
        accountMyBatisService.deleteUserByOauth2Id(OAUTH_ID, String.valueOf(leaving.getId()));

        em.clear();
        List<ChatMessage> messages =
                chatMessageRepository.findLatestByRoomId(ROOM_ID, PageRequest.of(0, 10));

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getMessage()).isEqualTo("먼저 갈게요");
        // 계정 연결은 끊기고
        assertThat(messages.get(0).getSender()).isNull();
        // 표시 이름은 남는다
        assertThat(messages.get(0).getSenderName()).isEqualTo("떠나는 사람");
    }
}
