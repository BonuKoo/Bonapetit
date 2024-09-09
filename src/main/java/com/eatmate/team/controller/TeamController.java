package com.eatmate.team.controller;

import com.eatmate.chat.service.ChatRoomService;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.team.service.TeamJpaService;
import com.eatmate.team.vo.TeamForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamJpaService teamJpaService;
    private final ChatRoomService chatRoomService;

    @PostMapping("/join/{teamId}")
    @ResponseBody
    TeamForm joinTeam(@PathVariable Long teamId, Principal principal){

        TeamForm teamForm = TeamForm.builder()
                .teamId(teamId)
                .userName(principal.getName())
                .build();

        TeamForm createdTeamForm = teamJpaService.joinTeam(teamForm);

        String userNickname = createdTeamForm.getUserNickname();
        String teamName = createdTeamForm.getTeamName();

        chatRoomService.enterChatRoom(createdTeamForm.getRoomId());

        //roomId를 반환하기 위해
        return createdTeamForm;
    }

}
