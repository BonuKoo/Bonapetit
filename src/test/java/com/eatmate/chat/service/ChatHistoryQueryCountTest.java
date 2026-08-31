package com.eatmate.chat.service;

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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * 방 진입 시 실제로 실행되는 쿼리 수를 센다.
 *
 * 캐시를 넣기 전에 "무엇이 비용인지"를 숫자로 고정해 두려는 목적이다.
 * 코드를 읽어 세는 것과 실제 실행은 다를 수 있다. 예컨대 ChatRoom.team은
 * @OneToOne 기본값이 EAGER라 조인으로 함께 오는지 별도 SELECT가 나가는지
 * 읽기만으로는 확정할 수 없다.
 *
 * 이 테스트는 성능 수치가 아니라 <b>쿼리 개수의 회귀를 막는 장치</b>다.
 * 연관관계 페치 전략이나 인가 로직이 바뀌어 쿼리가 늘면 여기서 걸린다.
 */
@DataJpaTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ChatHistoryQueryCountTest {

    private static final String ROOM_ID = "room-1";
    private static final String OAUTH_ID = "oauth-1";

    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountTeamRepository accountTeamRepository;
    @Autowired private TestEntityManager em;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Mock private ChatCacheRepository chatCacheRepository;

    private ChatHistoryService chatHistoryService;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        chatHistoryService = new ChatHistoryService(
                chatMessageRepository, chatRoomRepository, accountRepository,
                accountTeamRepository, chatCacheRepository);

        Account account = em.persist(Account.builder()
                .email("tester@eatmate.com").nickname("테스터").password("x").build());
        setOauth2Id(account, OAUTH_ID);

        Team team = em.persist(Team.builder().teamName("A팀").build());
        ChatRoom room = ChatRoom.builder().roomId(ROOM_ID).roomName("A팀 채팅방").team(team).build();
        em.persist(room);
        em.persist(AccountTeam.builder().account(account).team(team).isLeader(true).build());

        for (int i = 1; i <= 5; i++) {
            em.persist(ChatMessage.builder()
                    .chatRoom(room).sender(account).senderName("테스터")
                    .type(ChatMessage.MessageType.TALK).message("메시지 " + i).build());
        }

        em.flush();
        // 영속성 컨텍스트가 남아 있으면 조회가 1차 캐시로 처리되어 쿼리 수가 실제보다 적게 나온다.
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

    @Test
    @DisplayName("캐시가 없으면 방 진입에 쿼리 4건이 나간다 - 그중 3건이 인가 검증")
    void roomEntryWithoutCacheCostsFourQueries() {
        given(chatCacheRepository.isMemberVerified(ROOM_ID, OAUTH_ID)).willReturn(false);
        given(chatCacheRepository.getRecent(anyString(), anyInt())).willReturn(Optional.empty());

        chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        // ① 방 조회(team 포함) ② 계정 조회 ③ 멤버십 조회 ④ 메시지 조회
        //
        // 인가가 3/4를 차지한다는 것이 이 측정의 핵심이다. 메시지 조회만 캐싱하면
        // 4 -> 3으로 줄 뿐이므로, 멤버십까지 캐싱해야 의미 있는 개선이 된다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("멤버십만 캐시돼도 쿼리가 4건에서 1건으로 준다")
    void membershipCacheRemovesThreeQueries() {
        given(chatCacheRepository.isMemberVerified(ROOM_ID, OAUTH_ID)).willReturn(true);
        given(chatCacheRepository.getRecent(anyString(), anyInt())).willReturn(Optional.empty());

        chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        // 남는 것은 메시지 조회 1건뿐이다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("둘 다 캐시되면 DB 쿼리가 0건이다")
    void fullCacheHitCostsNoQuery() {
        given(chatCacheRepository.isMemberVerified(ROOM_ID, OAUTH_ID)).willReturn(true);
        given(chatCacheRepository.getRecent(anyString(), anyInt()))
                .willReturn(Optional.of(List.of()));

        chatHistoryService.getHistory(ROOM_ID, null, 10, OAUTH_ID);

        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    @DisplayName("위로 스크롤하는 구간은 캐시와 무관하게 메시지 쿼리가 나간다")
    void scrollBackAlwaysQueriesDatabase() {
        given(chatCacheRepository.isMemberVerified(ROOM_ID, OAUTH_ID)).willReturn(true);

        chatHistoryService.getHistory(ROOM_ID, 3L, 10, OAUTH_ID);

        // 첫 페이지만 캐싱하므로 스크롤 구간은 항상 DB를 탄다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
