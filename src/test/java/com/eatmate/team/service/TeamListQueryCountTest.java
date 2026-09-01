package com.eatmate.team.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 모임 목록 조회에 실제로 나가는 쿼리 수를 센다.
 *
 * [ISS-12]는 "10건 페이지당 최대 21회"라고 적고 있다. 그 숫자는 코드를 읽어
 * 센 것이라, 실제와 맞는지 확인한다.
 */
@DataJpaTest
@ActiveProfiles("test")
class TeamListQueryCountTest {

    /** 한 페이지 크기. TeamJpaService.getList가 PageRequest.of(page-1, 10)으로 고정한다. */
    private static final int PAGE_SIZE = 10;
    /** 모임당 참여자 수. 멤버 수가 쿼리 수에 영향을 주는지 보려고 1보다 크게 둔다. */
    private static final int MEMBERS_PER_TEAM = 3;

    @Autowired private AccountRepository accountRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private AccountTeamRepository accountTeamRepository;
    @Autowired private TestEntityManager em;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private TeamJpaService teamJpaService;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        teamJpaService = new TeamJpaService(accountRepository, teamRepository, accountTeamRepository);

        for (int t = 1; t <= PAGE_SIZE; t++) {
            Team team = em.persist(Team.builder().teamName("모임 " + t).placeName("장소 " + t).build());
            for (int m = 1; m <= MEMBERS_PER_TEAM; m++) {
                Account account = em.persist(Account.builder()
                        .email("t%d-m%d@eatmate.com".formatted(t, m))
                        .nickname("사용자 %d-%d".formatted(t, m))
                        .password("x").build());
                em.persist(AccountTeam.builder()
                        .account(account).team(team).isLeader(m == 1).build());
            }
        }

        em.flush();
        // 영속성 컨텍스트가 남아 있으면 1차 캐시로 처리되어 쿼리 수가 실제보다 적게 나온다.
        em.clear();

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @DisplayName("모임 목록 10건에 실제로 나가는 쿼리 수를 잰다")
    void measureListQueryCount() {
        var page = teamJpaService.getList(1, "");

        assertThat(page.getContent()).hasSize(PAGE_SIZE);
        assertThat(page.getContent().get(0).getMemCnt()).isEqualTo(MEMBERS_PER_TEAM);

        long count = statistics.getPrepareStatementCount();
        System.out.println("### 총 쿼리 = " + count);
        System.out.println("### JPQL 실행 = " + statistics.getQueryExecutionCount());
        for (String e : statistics.getEntityNames()) {
            long loads = statistics.getEntityStatistics(e).getLoadCount();
            if (loads > 0) System.out.println("### 엔티티 로드 " + e.substring(e.lastIndexOf('.') + 1) + " = " + loads);
        }
        for (String c : statistics.getCollectionRoleNames()) {
            long loads = statistics.getCollectionStatistics(c).getLoadCount();
            if (loads > 0) System.out.println("### 컬렉션 로드 " + c.substring(c.lastIndexOf('.') + 1) + " = " + loads);
        }

        // 실측값으로 고정한다. 늘어나면 여기서 걸린다.
        assertThat(count).isEqualTo(42);
    }

    /**
     * 개선안 시제품: 단일 프로젝션 쿼리로 같은 화면 데이터를 만든다.
     *
     * 엔티티를 안 만들면 EAGER 연관도 컬렉션도 따라오지 않는다.
     * 목록에 필요한 건 팀 정보 + 개설자 닉네임 + 인원수뿐이다.
     */
    @Test
    @DisplayName("개선안 - 프로젝션 한 방이면 몇 건인가")
    void measureProjectionAlternative() {
        var rows = em.getEntityManager().createQuery("""
                select t.id, t.teamName, t.addressName, t.roadAddressName, t.placeName,
                       leader.account.nickname, size(t.members), t.createdAt
                from Team t
                  join AccountTeam leader on leader.team = t and leader.isLeader = true
                where t.teamName like :kw or t.placeName like :kw
                   or t.addressName like :kw or t.roadAddressName like :kw
                order by t.createdAt desc
                """, Object[].class)
                .setParameter("kw", "%%")
                .setMaxResults(PAGE_SIZE)
                .getResultList();

        assertThat(rows).hasSize(PAGE_SIZE);
        assertThat(rows.get(0)[6]).isEqualTo(MEMBERS_PER_TEAM);   // 인원수
        assertThat(rows.get(0)[5]).isNotNull();                   // 개설자 닉네임

        System.out.println("### 개선안 쿼리 = " + statistics.getPrepareStatementCount() + " (count 쿼리 별도)");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
