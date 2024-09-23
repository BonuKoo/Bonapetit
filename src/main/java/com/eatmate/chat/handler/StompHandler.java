package com.eatmate.chat.handler;

import com.eatmate.chat.dto.ChatMessage;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.chat.service.ChatService;
import com.eatmate.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomRedisRepository chatRoomRedisRepository;
    private final ChatService chatService;

    //Websocket을 통해 들어온 요청이 처리 되기 전 실행
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        //webSocket 연결시 헤더의 jwt token 검증
        if (StompCommand.CONNECT == accessor.getCommand()){
            jwtTokenProvider.validateToken(accessor.getFirstNativeHeader("token"));
        } else if (StompCommand.SUBSCRIBE == accessor.getCommand()) { // 채팅룸 구독요청
            // header정보에서 구독 destination정보를 얻고, roomId를 추출한다.
            String roomId = chatService.getRoomId(Optional.ofNullable((String) message.getHeaders().get("simpDestination")).orElse("InvalidRoomId"));
            // 채팅방에 들어온 클라이언트 sessionId를 roomId와 맵핑해 놓는다.(나중에 특정 세션이 어떤 채팅방에 들어가 있는지 알기 위함)
            String sessionId = (String) message.getHeaders().get("simpSessionId");
            chatRoomRedisRepository.setUserEnterInfo(sessionId, roomId);
            // 채팅방의 인원수를 +1한다.
            chatRoomRedisRepository.plusUserCount(roomId);
            // 클라이언트 입장 메시지를 채팅방에 발송한다.(redis publish)

            String nickname = Optional.ofNullable((OAuth2AuthenticationToken) message.getHeaders().get("simpUser"))
                    .map(auth -> (String) auth.getPrincipal()
                            .getAttributes()
                            .get("nickname"))
                    .orElse("UnknownUser");

            chatService.sendChatMessage(ChatMessage.builder().type(ChatMessage.MessageType.ENTER).roomId(roomId).sender(nickname).build());
            log.info("SUBSCRIBED {}, {}", nickname, roomId);
        } else if (StompCommand.DISCONNECT == accessor.getCommand()) { // Websocket 연결 종료
            // 연결이 종료된 클라이언트 sesssionId로 채팅방 id를 얻는다.
            String sessionId = (String) message.getHeaders().get("simpSessionId");

            String roomId = chatRoomRedisRepository.getUserEnterRoomId(sessionId);

            // 채팅방의 인원수를 -1한다.
            chatRoomRedisRepository.minusUserCount(roomId);

            // 클라이언트 퇴장 메시지를 채팅방에 발송한다.(redis publish)
            String nickname = Optional.ofNullable((OAuth2AuthenticationToken) message.getHeaders().get("simpUser"))
                    .map(auth -> (String) auth.getPrincipal()
                            .getAttributes()
                            .get("nickname"))
                    .orElse("UnknownUser");

            chatService.sendChatMessage(ChatMessage.builder()
                    .type(ChatMessage.MessageType.QUIT)
                    .roomId(roomId)
                    .sender(nickname).build());
            // 퇴장한 클라이언트의 roomId 맵핑 정보를 삭제한다.
            chatRoomRedisRepository.removeUserEnterInfo(sessionId);
            log.info("DISCONNECTED {}, {}", sessionId, roomId);
        }

        return message;
    }
}
