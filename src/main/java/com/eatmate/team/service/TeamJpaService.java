package com.eatmate.team.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.team.vo.TeamForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamJpaService {

    private final TeamRepository teamRepository;
    private final AccountRepository accountRepository;

    /**
     * Create
    */
    @Transactional
    public Team createTeamWhenUserCreatePost(String teamName,String email){

        // 1. Account를 이메일로 검색
        Account account = accountRepository.findByEmail(email);
        if (account == null){
            throw new IllegalArgumentException("해당 이메일을 가진 계정이 없습니다.");
        }

        // 2. Team 생성
        Team team = Team.builder()
                .teamName(teamName)
                .build();

        // 3. AccountTeam 생성 및 Leader 등록
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