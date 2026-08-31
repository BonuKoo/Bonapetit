package com.eatmate.team.service;

import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.team.vo.TeamMembership;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 인가 검사에 실제로 나가는 쿼리 수를 센다.
 *
 * 이 프로젝트는 "추정하지 말고 측정한다"를 지켜 왔다. 인가 검사는 모임을 다루는
 * 모든 요청 앞에 붙으므로, 검사 한 번의 비용을 숫자로 고정해 둔다.
 *
 * 기존 {@code findByAccountAndTeam}은 Account·Team 엔티티를 인자로 받아 호출부가
 * 두 건을 먼저 조회해야 한다(채팅 경로가 그래서 인가에만 3쿼리를 쓴다 →
 * {@code ChatHistoryQueryCountTest}). 식별자로 바로 묻는 쿼리를 새로 둔 이유가 그것이다.
 *
 * 이 테스트가 확인하는 것은 쿼리 <b>개수</b>이지 실행 계획이 아니다. H2와 MySQL은
 * 실행 계획이 갈리므로 성능 판단은 여기서 하지 않는다.
 */
@DataJpaTest
@ActiveProfiles("test")
class TeamAccessQueryCountTest {

    private static final String LEADER_OAUTH_ID = "leader-oauth-id";
    private static final String MEMBER_OAUTH_ID = "member-oauth-id";
    private static final String OUTSIDER_OAUTH_ID = "outsider-oauth-id";

    @Autowired private AccountTeamRepository accountTeamRepository;
    @Autowired private TestEntityManager em;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private TeamAccessService teamAccessService;
    private Statistics statistics;
    private Long teamId;

    @BeforeEach
    void setUp() {
        teamAccessService = new TeamAccessService(accountTeamRepository);

        Team team = em.persist(Team.builder().teamName("A팀").build());
        teamId = team.getId();

        em.persist(AccountTeam.builder()
                .account(account("leader@eatmate.com", "개설자", LEADER_OAUTH_ID))
                .team(team).isLeader(true).build());
        em.persist(AccountTeam.builder()
                .account(account("member@eatmate.com", "참여자", MEMBER_OAUTH_ID))
                .team(team).isLeader(false).build());
        // 다른 팀에만 속한 사람. 이 팀에는 아무 권한이 없다.
        Team otherTeam = em.persist(Team.builder().teamName("B팀").build());
        em.persist(AccountTeam.builder()
                .account(account("outsider@eatmate.com", "외부인", OUTSIDER_OAUTH_ID))
                .team(otherTeam).isLeader(true).build());

        em.flush();
        // 영속성 컨텍스트가 남아 있으면 조회가 1차 캐시로 처리되어 쿼리 수가 적게 나온다.
        em.clear();

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    private Account account(String email, String nickname, String oauth2Id) {
        Account account = em.persist(Account.builder()
                .email(email).nickname(nickname).password("x").build());
        // oauth2id는 빌더에 없어 리플렉션으로 넣는다.
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
    @DisplayName("개설자 검사는 쿼리 1건이다")
    void leaderCheckCostsOneQuery() {
        TeamMembership membership = teamAccessService.requireLeader(teamId, LEADER_OAUTH_ID);

        assertThat(membership.leader()).isTrue();
        // 계정 조회 · 팀 조회 없이 account_team 한 건만 본다.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("멤버 검사도 쿼리 1건이다")
    void memberCheckCostsOneQuery() {
        teamAccessService.requireMember(teamId, MEMBER_OAUTH_ID);

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("멤버십에서 얻은 account_id는 세션 주체의 실제 계정 식별자다")
    void membershipCarriesRealAccountId() {
        // 컨트롤러가 요청 파라미터 대신 이 값을 쓴다. 실제 값이 나오는지 확인한다.
        Account member = em.getEntityManager()
                .createQuery("select a from Account a where a.oauth2id = :id", Account.class)
                .setParameter("id", MEMBER_OAUTH_ID)
                .getSingleResult();

        TeamMembership membership = teamAccessService.requireMember(teamId, MEMBER_OAUTH_ID);

        assertThat(membership.accountId()).isEqualTo(member.getId());
        assertThat(membership.leader()).isFalse();
    }

    @Test
    @DisplayName("다른 팀 사람은 멤버 검사에서 걸러진다")
    void outsiderIsDenied() {
        assertThatThrownBy(() -> teamAccessService.requireMember(teamId, OUTSIDER_OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("멤버지만 개설자가 아니면 개설자 검사에서 걸러진다")
    void memberIsNotLeader() {
        assertThatThrownBy(() -> teamAccessService.requireLeader(teamId, MEMBER_OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);
    }
}
