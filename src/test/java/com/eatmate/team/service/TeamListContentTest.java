package com.eatmate.team.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.team.vo.TeamVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 모임 목록의 <b>내용</b>이 맞는지 본다.
 *
 * 쿼리 수는 {@link TeamListQueryCountTest}가 센다. 쿼리를 42건에서 2건으로 줄이면서
 * 엔티티 순회를 프로젝션으로 바꿨으므로, 같은 화면이 나오는지는 따로 확인해야 한다.
 */
@DataJpaTest
@ActiveProfiles("test")
class TeamListContentTest {

    @Autowired private AccountRepository accountRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private AccountTeamRepository accountTeamRepository;
    @Autowired private TestEntityManager em;

    private TeamJpaService teamJpaService;

    @BeforeEach
    void setUp() {
        teamJpaService = new TeamJpaService(accountRepository, teamRepository, accountTeamRepository);
    }

    private Account account(String nickname) {
        return em.persist(Account.builder()
                .email(nickname + "@eatmate.com").nickname(nickname).password("x").build());
    }

    /** 개설자 1명 + 참여자 memberCount-1 명으로 모임을 만든다. */
    private Team team(String name, String placeName, String leaderNickname, int memberCount) {
        Team team = em.persist(Team.builder().teamName(name).placeName(placeName)
                .addressName(name + " 주소").roadAddressName(name + " 도로명").build());
        em.persist(AccountTeam.builder()
                .account(account(leaderNickname)).team(team).isLeader(true).build());
        for (int i = 1; i < memberCount; i++) {
            em.persist(AccountTeam.builder()
                    .account(account(name + "참여자" + i)).team(team).isLeader(false).build());
        }
        return team;
    }

    @Test
    @DisplayName("개설자 닉네임과 인원수가 모임마다 정확히 붙는다")
    void authorAndMemberCountArePerTeam() {
        team("삼겹살 모임", "고깃집", "김개설", 3);
        team("파스타 모임", "이탈리안", "이개설", 1);
        em.flush();
        em.clear();

        Page<TeamVo> page = teamJpaService.getList(1, "");

        assertThat(page.getContent())
                .extracting(TeamVo::getTeamName, TeamVo::getAuthor, TeamVo::getMemCnt)
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("삼겹살 모임", "김개설", 3),
                        org.assertj.core.api.Assertions.tuple("파스타 모임", "이개설", 1));
    }

    @Test
    @DisplayName("검색어는 이름·장소·주소에 걸린다")
    void keywordMatchesNamePlaceAndAddress() {
        team("삼겹살 모임", "고깃집", "김개설", 1);
        team("파스타 모임", "이탈리안", "이개설", 1);
        em.flush();
        em.clear();

        assertThat(teamJpaService.getList(1, "삼겹살").getContent())
                .extracting(TeamVo::getTeamName).containsExactly("삼겹살 모임");
        // 장소명으로도 걸려야 한다
        assertThat(teamJpaService.getList(1, "이탈리안").getContent())
                .extracting(TeamVo::getTeamName).containsExactly("파스타 모임");
        assertThat(teamJpaService.getList(1, "없는검색어").getContent()).isEmpty();
    }

    @Test
    @DisplayName("전체 건수와 실제 항목 수가 어긋나지 않는다")
    void totalMatchesContent() {
        for (int i = 1; i <= 3; i++) {
            team("모임 " + i, "장소 " + i, "개설자" + i, 2);
        }
        // 개설자가 없는 모임. 예전에는 목록에서 건너뛰면서도 전체 건수에는 남아
        // 페이지에 빈자리를 만들었다.
        em.persist(Team.builder().teamName("리더 없는 모임").placeName("어딘가").build());
        em.flush();
        em.clear();

        Page<TeamVo> page = teamJpaService.getList(1, "");

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(TeamVo::getTeamName)
                .doesNotContain("리더 없는 모임");
    }

    @Test
    @DisplayName("표시용 날짜는 yyyy/MM/dd 형식이다")
    void createdDateIsFormatted() {
        team("삼겹살 모임", "고깃집", "김개설", 1);
        em.flush();
        em.clear();

        assertThat(teamJpaService.getList(1, "").getContent().get(0).getCreatedDate())
                .matches("\\d{4}/\\d{2}/\\d{2}");
    }
}
