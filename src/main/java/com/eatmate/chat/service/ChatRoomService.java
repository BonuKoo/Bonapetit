package com.eatmate.chat.service;

import com.eatmate.chat.dto.ChatRoomDTO;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.domain.entity.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRedisRepository chatRoomRedisRepository;
    
    //채팅방을 생성
    @Transactional
    public void connectAndCreateChatRoom(ChatRoomDTO chatRoomDTO){

        //방 생성
        chatRoomRedisRepository.createChatRoom(chatRoomDTO);

        /*
        //입장
        enterChatRoom(chatRoomDTO.getRoomId());
        */

    }

    //채팅방에 입장
    /*
    @Transactional
    public void enterChatRoom(String roomId){
        chatRoomRedisRepository.enterChatRoom(roomId);
    }
    */

}
