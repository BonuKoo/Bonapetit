package com.eatmate.team.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.team.vo.TeamForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.ldap.PagedResultsControl;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamJpaService {


    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final AccountTeamRepository accountTeamRepository;
    /*
    @Transactional
    public TeamForm joinTeam(TeamForm teamForm){

        Account account = accountRepository.findByOauth2id(teamForm.getUserName());

        Team team = teamRepository.findById(teamForm.getTeamId()).get();

        AccountTeam accountTeam = AccountTeam.builder()
                .account(account)
                //.team(team)
                .isLeader(false)
                .build();

        team.addAccountTeam(accountTeam);

        Team savedTeam = teamRepository.save(team);

        //RoomId, 공통 닉네임, 팀 이름을 저장 후 반환
        teamForm.attachRoomIdAndNickname(
                savedTeam.getChatRoom().getRoomId(),
                account.getNickname(),
                savedTeam.getTeamName());

        return teamForm;

    }
    */

    @Transactional
    public TeamForm joinTeam(TeamForm teamForm){

        Account account = accountRepository.findByOauth2id(teamForm.getUserName());
        Team team = teamRepository.findById(teamForm.getTeamId()).orElseThrow(() -> new IllegalArgumentException("Invalid team ID"));

        // 사용자가 이미 팀에 속해있는지 확인
        Optional<AccountTeam> existingAccountTeam = accountTeamRepository.findByAccountAndTeam(account, team);

        if (existingAccountTeam.isPresent()) {
            // 이미 팀에 가입되어 있으면 바로 팀 정보를 반환
            teamForm.attachRoomIdAndNickname(
                    team.getChatRoom().getRoomId(),
                    account.getNickname(),
                    team.getTeamName());
            return teamForm;
        }

        // 가입되어 있지 않으면 새로운 AccountTeam 엔티티를 생성
        AccountTeam accountTeam = AccountTeam.builder()
                .account(account)
                .team(team)
                .isLeader(false)
                .build();

        team.addAccountTeam(accountTeam);
        teamRepository.save(team);

        teamForm.attachRoomIdAndNickname(
                team.getChatRoom().getRoomId(),
                account.getNickname(),
                team.getTeamName());

        return teamForm;
    }

}
