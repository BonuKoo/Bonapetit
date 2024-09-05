package com.eatmate.chat.controller;

import com.eatmate.chat.dto.ChatRoomDTO;

import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.domain.entity.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/chat")
public class ChatRoomController {

    private final ChatRoomRedisRepository chatRoomRedisRepository;

    /*
    채팅방 생성 -> Team 만들 때 채팅방은 생성됨.
    @PostMapping("/room")
    @ResponseBody
    public ChatRoomDTO createRoom(
            @RequestParam String name
    ) {

        return chatRoomRedisRepository.createChatRoom(name);
    }
    */

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

}
