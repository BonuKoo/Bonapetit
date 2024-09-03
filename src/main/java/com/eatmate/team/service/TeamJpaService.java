package com.eatmate.team.service;

import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamJpaService {

    private final TeamRepository teamRepository;

    /**
     * Create
    */
    @Transactional
    public Team createTeamWhenUserCreatePostAndChatRoom(String teamName, Account account){
        // Team 생성
        Team team = Team.builder()
                .teamName(teamName)
                .build();

        // AccountTeam 생성 및 Leader 등록
        AccountTeam accountTeam = AccountTeam.builder()
                .account(account)
                .team(team)
                .isLeader(true)
                .build();

        // AccountTeam을 Team에 추가.
        team.addAccountTeam(accountTeam);

        // Team 저장
        return teamRepository.save(team);
    }

}