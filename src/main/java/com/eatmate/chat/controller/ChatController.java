package com.eatmate.chat.controller;

import com.eatmate.chat.dto.ChatMessage;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final ChannelTopic channelTopic;

    /**
     * webSocket "/pub/chat/message"로 들어오는 메시징을 처리
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessage message, @Header("token") String token) {
        String nickname = jwtTokenProvider.getNicknameFromJwt(token);
        // 로그인 회원 정보로 대화명 설정
        message.setSender(nickname);
        // 채팅방 입장시에는 대화명과 메시지를 자동으로 세팅한다.
        if (ChatMessage.MessageType.ENTER.equals(message.getType())) {
            message.setSender("[알림]");
            message.setMessage(nickname + "님이 입장하셨습니다.");
        }
        // Websocket에 발행된 메시지를 redis로 발행(publish)
        redisTemplate.convertAndSend(channelTopic.getTopic(), message);
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