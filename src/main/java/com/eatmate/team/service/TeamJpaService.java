package com.eatmate.team.service;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.team.vo.TeamForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamJpaService {


    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public TeamForm joinTeam(TeamForm teamForm){


        Account account = accountRepository.findByOauth2id(teamForm.getUserName());

        log.info("account ID : {}", account);
        log.info("account ID : {}", account.getId().toString());

        Team team = teamRepository.findById(teamForm.getTeamId()).get();

        AccountTeam accountTeam = AccountTeam.builder()
                .account(account)
                .team(team)
                .isLeader(false)
                .build();

        team.addAccountTeam(accountTeam);

        Team savedTeam = teamRepository.save(team);

        teamForm.joinRoomId(savedTeam.getChatRoom().getRoomId());

        return teamForm;

    }

}
