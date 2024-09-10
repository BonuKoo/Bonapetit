package com.eatmate.dao.repository.team;

import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTeamRepository extends JpaRepository<AccountTeam,Long> {
    Optional<AccountTeam> findByAccountAndTeam(Account account, Team team);
}
