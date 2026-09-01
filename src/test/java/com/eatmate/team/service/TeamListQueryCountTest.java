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
 * ISS-12는 "10건 페이지당 최대 21회"라고 적고 있었다. 그 숫자는 코드를 읽어 센
 * 것이었고, 실제로 재 보니 <b>42회</b>였다. 내역은 이랬다.
 *
 * <pre>
 *   JPQL 12   목록 1 + count 1 + 개설자 조회 10
 *   컬렉션 10  Team.getMembersCount()가 members 를 통째로 로드
 *   나머지 20  AccountTeam 의 @ManyToOne 이 fetch 미지정이라 EAGER
 * </pre>
 *
 * 프로젝션으로 바꾼 뒤 <b>2회</b>다(목록 1 + count 1). 이 테스트가 그 숫자를 고정한다.
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
    @DisplayName("모임 목록 10건이 쿼리 2건으로 끝난다")
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

        // 목록 1 + count 1. 항목 수에 비례해 늘어나는 쿼리가 없어야 한다.
        assertThat(count).isEqualTo(2);
    }
}
