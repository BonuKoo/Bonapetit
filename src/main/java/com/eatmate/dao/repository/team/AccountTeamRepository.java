package com.eatmate.dao.repository.team;

import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.team.vo.TeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountTeamRepository extends JpaRepository<AccountTeam, Long> {

    // account와 team을 기준으로 이미 존재하는지 확인하는 메서드
    Optional<AccountTeam> findByAccountAndTeam(Account account, Team team);

    @Query("select a " +
            "from AccountTeam a " +
            "where a.team.id = :teamId " +
            "and a.isLeader = true")
    AccountTeam findLeaderAccountTeamByTeamId(@Param("teamId") Long teamId);

    /**
     * 인가 검사용 멤버십 조회.
     *
     * findByAccountAndTeam은 Account·Team 엔티티를 인자로 받아 호출부가 두 건을 먼저
     * 조회해야 한다. 인가 검사는 식별자만 알면 되므로 바로 묻는다.
     *
     * 엔티티가 아니라 프로젝션으로 받는 이유는 측정 때문이다. AccountTeam을 그대로
     * 돌려주면 account·team 연관이 EAGER라 SELECT가 4건 나갔다. 필요한 두 값만
     * 뽑으면 1건이다({@code TeamAccessQueryCountTest}).
     */
    @Query("select new com.eatmate.team.vo.TeamMembership(a.account.id, a.isLeader) " +
            "from AccountTeam a " +
            "where a.team.id = :teamId " +
            "and a.account.oauth2id = :oauth2Id")
    Optional<TeamMembership> findMembership(@Param("teamId") Long teamId,
                                            @Param("oauth2Id") String oauth2Id);
}
