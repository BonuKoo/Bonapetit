package com.eatmate.post.service;

import com.eatmate.chat.dto.ChatRoomDTO;
import com.eatmate.chat.service.ChatRoomService;
import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.team.CustomTeamRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.post.vo.PostForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostJpaService {

    private final AccountDao accountDao;
    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    
    private final CustomTeamRepository customTeamRepository;

    private final ChatRoomService chatRoomService;

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
                 .build();

         //팀에 할당되는 ChatRoom 생성

        /*
        StringBuffer stringBuffer = new StringBuffer();
        String chatRoomName = stringBuffer.append(form.getTeamName()).append(" 의 채팅방").toString();
         */

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(UUID.randomUUID().toString())
                .roomName(form.getTeamName())
                .build();

        AccountTeam accountTeam = AccountTeam.builder()
                 .account(account)
                 .team(team)
                 .isLeader(true)
                 .build();

        team.setChatRoom(chatRoom);
        chatRoom.setTeam(team);

        ChatRoomDTO chatRoomDTO = customTeamRepository.createTeamAndChatRoomThenReturnChatRoomDto(team);

        chatRoomService.connectAndCreateChatRoom(chatRoomDTO);

    }

}
