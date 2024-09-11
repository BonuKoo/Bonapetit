package com.eatmate.dao.repository.team;

import com.eatmate.domain.entity.user.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team,Long>
    ,CustomTeamRepository4QueryDsl
{ }
