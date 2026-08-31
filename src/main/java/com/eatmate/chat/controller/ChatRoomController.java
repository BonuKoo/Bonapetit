package com.eatmate.chat.controller;

import com.eatmate.chat.dto.ChatHistoryResponse;
import com.eatmate.chat.dto.ChatRoomDTO;

import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.chat.service.ChatHistoryService;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.jwt.JwtTokenProvider;
import com.eatmate.team.service.TeamAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/chat")
public class ChatRoomController {

    private final ChatRoomRedisRepository chatRoomRedisRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatHistoryService chatHistoryService;
    private final TeamAccessService teamAccessService;

    /**
     * 채팅 내역 조회 (커서 기반).
     *
     * 최초 진입 시에는 before 없이 호출해 최신 size건을 받고, 위로 스크롤할 때
     * 직전 응답의 nextCursor를 before로 넘겨 그 이전 구간을 받는다.
     *
     * principal.getName()이 곧 oauth2Id다. CustomOAuth2UserService가
     * userNameAttributeKey(kakao/naver는 id, google은 sub)를 oauth2Id와
     * 같은 값으로 맞춰 두었다.
     */
    @GetMapping("/room/{roomId}/messages")
    @ResponseBody
    public ChatHistoryResponse messages(@PathVariable String roomId,
                                        @RequestParam(required = false) Long before,
                                        @RequestParam(required = false) Integer size,
                                        Principal principal) {
        return chatHistoryService.getHistory(roomId, before, size, principal.getName());
    }

    //채팅방 입장 화면
    @GetMapping("/room/enter/{roomId}")
    public String roomDetail(Model model, @PathVariable String roomId){
        model.addAttribute("roomId",roomId);
        return "chat/roomdetail";
    }

    //특정 채팅방 조회
    @GetMapping("/room/{roomId}")
    @ResponseBody
    public ChatRoomDTO roomInfo(@PathVariable String roomId){
        return chatRoomRedisRepository.findRoomById(roomId);
    }

    //채팅 리스트 화면
    @GetMapping("/room")
    public String rooms(Model model){
        return "chat/room";
    }

    //모든 채팅방 목록 반환
    @GetMapping("/rooms")
    @ResponseBody
    public List<ChatRoomDTO> room(){
        return chatRoomRedisRepository.findAllRoom();
    }

    /**
     * profileListForm에서 채팅방 정보를 불러온다.
     *
     * teamId만 받고 멤버십을 확인하지 않아 남의 팀 채팅방 정보(roomId·방 이름)를
     * 그대로 내주고 있었다. roomId는 구독과 내역 조회의 입력이므로 넘겨주지 않는다.
     */
    @PostMapping("/enter/{teamId}")
    @ResponseBody
    public ChatRoomDTO enterRoom(
            @PathVariable Long teamId,
            Principal principal){

        teamAccessService.requireMember(teamId, principal.getName());

        ChatRoom chatRoom = chatRoomRepository.findByTeam(teamId);

        return ChatRoomDTO.builder()
                .roomName(chatRoom.getRoomName())
                .roomId(chatRoom.getRoomId())
                .build();

    }
}
