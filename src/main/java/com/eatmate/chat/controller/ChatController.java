package com.eatmate.chat.controller;

import com.eatmate.chat.dto.ChatMessage;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.domain.entity.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/chat/message")
    public void message(ChatMessage message){
        if (ChatMessage.MessageType.JOIN.equals(message.getType()))
            message.setMessage(message.getSender() + "님이 입장했습니다.");
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId());
    }

}

/**
 * @MessageMapping을 통해 WebSocket으로 들어오는 메시지 발행을 처리
 * 클라이언트에서 prefix를 붙여서
 * /pub/chat/message로 발행 요청을 하면
 * Controller가 해당 메시지를 받아서 처리한다.
 * 메시지라 발행되면
 * /sub/chat/room/{roomId}로 메시지를 send한다.
 *
 * 클라이언트에서는 해당 주소를 (/sub/chat/room/{roomId}) 구독(sub)하고 있다가
 * 메시지가 전달되면 화면에 출력한다.
 *
 * 여기서, /sub/chat/room/{roomId} 는 채팅룸을 구분하는 값이므로,
 * pub/sub에서 Topic의 역할이다.
 *
 * -> WebSocketChatHandler의 역항를 대체하므로,
 * WebSocketChatHandler는 삭제한다.
 */