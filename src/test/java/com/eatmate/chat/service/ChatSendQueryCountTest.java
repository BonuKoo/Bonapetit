package com.eatmate.chat.service;

import com.eatmate.chat.dto.ChatMessageDTO;
import com.eatmate.chat.redisDao.ChatCacheRepository;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chat.ChatMessageRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 메시지 한 건을 보낼 때 실제로 나가는 DB 쿼리 수를 센다.
 *
 * 채팅에서 가장 뜨거운 경로다. 여기서 쿼리 1건은 메시지 1건마다의 비용이라,
 * 목록 화면의 쿼리 1건과 무게가 다르다.
 *
 * Redis 왕복은 이 테스트가 세지 않는다(목으로 대체). 왕복 횟수는 각
 * 저장소 메서드가 실행하는 명령 수를 코드에서 세어 문서에 적는다.
 */
@DataJpaTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ChatSendQueryCountTest {

    private static final String ROOM_ID = "room-1";
    private static final String OAUTH_ID = "oauth-1";

    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TestEntityManager em;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Mock private ChannelTopic channelTopic;
    @Mock private RedisTemplate redisTemplate;
    @Mock private ChatRoomRedisRepository chatRoomRedisRepository;
    @Mock private ChatCacheRepository chatCacheRepository;

    private ChatService chatService;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(channelTopic, redisTemplate, chatRoomRedisRepository,
                chatMessageRepository, chatRoomRepository, accountRepository, chatCacheRepository);

        Account account = em.persist(Account.builder()
                .email("tester@eatmate.com").nickname("테스터").password("x").build());
        setOauth2Id(account, OAUTH_ID);

        Team team = em.persist(Team.builder().teamName("A팀").build());
        ChatRoom room = ChatRoom.builder().roomId(ROOM_ID).roomName("A팀 채팅방").team(team).build();
        em.persist(room);
        em.persist(AccountTeam.builder().account(account).team(team).isLeader(true).build());

        em.flush();
        // 영속성 컨텍스트가 남아 있으면 1차 캐시로 처리되어 쿼리 수가 실제보다 적게 나온다.
        em.clear();

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    /** oauth2id는 빌더에 없어 리플렉션으로 넣는다. */
    private void setOauth2Id(Account account, String value) {
        try {
            var f = Account.class.getDeclaredField("oauth2id");
            f.setAccessible(true);
            f.set(account, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private ChatMessageDTO talk(String text) {
        return ChatMessageDTO.builder()
                .type(ChatMessage.MessageType.TALK).roomId(ROOM_ID).sender("테스터").message(text).build();
    }

    private void report(String label) {
        System.out.println("### " + label + " 총 쿼리 = " + statistics.getPrepareStatementCount());
        for (String e : statistics.getEntityNames()) {
            long l = statistics.getEntityStatistics(e).getLoadCount();
            long i = statistics.getEntityStatistics(e).getInsertCount();
            if (l > 0 || i > 0) {
                System.out.println("###   " + e.substring(e.lastIndexOf('.') + 1)
                        + " load=" + l + " insert=" + i);
            }
        }
    }

    @Test
    @DisplayName("TALK 한 건을 보낼 때 쿼리가 몇 건인가")
    void talkMessageCost() {
        chatService.sendChatMessage(talk("안녕하세요"), OAUTH_ID);
        em.flush();

        report("TALK 1건");
        // ① 발신 계정 조회(SELECT) ② 메시지 저장(INSERT)
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("연속으로 3건 보내면 쿼리가 몇 건인가")
    void threeMessagesCost() {
        for (int i = 1; i <= 3; i++) {
            chatService.sendChatMessage(talk("메시지 " + i), OAUTH_ID);
        }
        em.flush();

        report("TALK 3건");
        // 메시지당 2건. 계정 조회는 파생 쿼리라 영속성 컨텍스트에 계정이 이미 있어도
        // 매번 SQL이 나간다(Account load=1 인데 statement 는 3건).
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("ENTER/QUIT은 저장하지 않으므로 쿼리가 없어야 한다")
    void enterCostsNothing() {
        chatService.sendChatMessage(ChatMessageDTO.builder()
                .type(ChatMessage.MessageType.ENTER).roomId(ROOM_ID).sender("테스터").build(), null);

        report("ENTER 1건");
        // 입퇴장 알림은 저장하지 않는다. 재접속이 잦으면 실제 대화보다 많아지기 때문이다.
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }
}
