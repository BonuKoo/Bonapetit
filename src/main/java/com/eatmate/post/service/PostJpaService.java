package com.eatmate.post.service;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.post.vo.PostForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostJpaService {

    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final AccountDao accountDao;

    /**
     * 게시글 생성
     */

    @Transactional
     public void createChatRoomAndTeamWhenWriteThePost(PostForm form){

        AccountDto accountDto = accountDao.findByOauth2Id(form.getAuthor());

        Account account = accountRepository.findById(accountDto.getAccount_id()).get();

        //팀 만들기
         Team team = Team.builder()
                 .teamName(form.getTeamName())
                 .description(form.getDescription())
                 .location(form.getLocation())
                 .leader(account)
                 .build();

         AccountTeam accountTeam = AccountTeam.builder()
                 .account(account)
                 .team(team)
                 .isLeader(true)
                 .build();

         teamRepository.save(team);
     }

}
