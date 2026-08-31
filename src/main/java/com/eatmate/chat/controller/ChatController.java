package com.eatmate.chat.controller;

import com.eatmate.chat.dto.ChatMessageDTO;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.chat.service.ChatService;
import com.eatmate.jwt.JwtTokenProvider;
import com.eatmate.post.vo.PostForm;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomRedisRepository chatRoomRedisRepository;
    private final ChatService chatService;

    /**
     * webSocket "/pub/chat/message"로 들어오는 메시징을 처리
     */

    @MessageMapping("/chat/message")
    public void message(ChatMessageDTO message, @Header("token") String token) {
        // 로그인 회원 정보로 대화명 설정
        message.setSender(jwtTokenProvider.getNicknameFromJwt(token));
        // 저장 시 발신 계정을 연결하기 위해 oauth2Id도 함께 넘긴다.
        // userCount 세팅은 sendChatMessage가 하므로 여기서 하지 않는다
        // (이전엔 양쪽에서 호출해 메시지당 Redis 왕복이 1회 낭비되고 있었다).
        chatService.sendChatMessage(message, jwtTokenProvider.getOauthIdFromJwt(token));

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
 * -> WebSocketChatHandler의 역할을 대체하므로 삭제됨.
 */