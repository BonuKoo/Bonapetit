package com.eatmate.chat.controller;

import com.eatmate.chat.dto.ChatRoomDTO;

import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.jwt.JwtTokenProvider;
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

    //profileListForm에서 채팅방 정보를 불러온다.
    @PostMapping("/enter/{teamId}")
    @ResponseBody
    public ChatRoomDTO enterRoom(
            @PathVariable Long teamId,
            Principal principal){

        ChatRoom chatRoom = chatRoomRepository.findByTeam(teamId);

        ChatRoomDTO chatRoomDTO = ChatRoomDTO.builder()
                .roomName(chatRoom.getRoomName())
                .roomId(chatRoom.getRoomId())
                .build();
        return chatRoomDTO;
    }


}
