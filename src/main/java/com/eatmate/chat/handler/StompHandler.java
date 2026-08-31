package com.eatmate.chat.handler;

import com.eatmate.chat.dto.ChatMessageDTO;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.chat.service.ChatRoomMembershipVerifier;
import com.eatmate.chat.service.ChatService;
import com.eatmate.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    /**
     * 브로커에서 채팅방을 나타내는 destination 접두사.
     *
     * 구독 경로를 화이트리스트로 두는 이유는, 마지막 경로 조각을 roomId로 삼는 방식이
     * "/sub/아무거나"에도 그대로 걸려 방 진입 처리가 엉뚱하게 실행되기 때문이다.
     */
    private static final String CHAT_ROOM_DESTINATION_PREFIX = "/sub/chat/room/";

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomRedisRepository chatRoomRedisRepository;
    private final ChatService chatService;
    private final ChatRoomMembershipVerifier membershipVerifier;

    //Websocket을 통해 들어온 요청이 처리 되기 전 실행
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        //webSocket 연결시 헤더의 jwt token 검증
        if (StompCommand.CONNECT == accessor.getCommand()){
            jwtTokenProvider.validateToken(accessor.getFirstNativeHeader("token"));
        } else if (StompCommand.SUBSCRIBE == accessor.getCommand()) { // 채팅룸 구독요청
            String destination = (String) message.getHeaders().get("simpDestination");
            if (destination == null || !destination.startsWith(CHAT_ROOM_DESTINATION_PREFIX)) {
                // 채팅방 구독이 아니면 방 진입 처리를 하지 않고 그대로 흘려보낸다.
                return message;
            }

            // 접두사가 확인됐으므로 나머지가 곧 roomId다.
            String roomId = destination.substring(CHAT_ROOM_DESTINATION_PREFIX.length());

            // 구독은 그 방의 대화를 실시간으로 받는다는 뜻이므로, 내역 조회와 똑같이
            // 멤버십을 확인해야 한다. 이게 없으면 roomId만 알면 남의 대화를 열람할 수 있다.
            // 여기서 던진 예외는 STOMP ERROR 프레임이 되어 구독이 성립하지 않는다.
            membershipVerifier.verify(roomId, subscriberOauth2Id(message));

            // 채팅방에 들어온 클라이언트 sessionId를 roomId와 맵핑해 놓는다.(나중에 특정 세션이 어떤 채팅방에 들어가 있는지 알기 위함)
            String sessionId = (String) message.getHeaders().get("simpSessionId");
            chatRoomRedisRepository.setUserEnterInfo(sessionId, roomId);
            // 채팅방의 인원수를 +1한다.
            chatRoomRedisRepository.plusUserCount(roomId);
            // 클라이언트 입장 메시지를 채팅방에 발송한다.(redis publish)

            String nickname = nickname(message);

            chatService.sendChatMessage(ChatMessageDTO.builder().type(ChatMessage.MessageType.ENTER).roomId(roomId).sender(nickname).build(), null);
            log.info("SUBSCRIBED {}, {}", nickname, roomId);
        } else if (StompCommand.DISCONNECT == accessor.getCommand()) { // Websocket 연결 종료
            // 연결이 종료된 클라이언트 sesssionId로 채팅방 id를 얻는다.
            String sessionId = (String) message.getHeaders().get("simpSessionId");

            String roomId = chatRoomRedisRepository.getUserEnterRoomId(sessionId);

            // 구독이 거부돼 방에 들어간 적이 없는 세션은 퇴장 처리할 것도 없다.
            if (roomId == null) {
                return message;
            }

            // 채팅방의 인원수를 -1한다.
            chatRoomRedisRepository.minusUserCount(roomId);

            // 클라이언트 퇴장 메시지를 채팅방에 발송한다.(redis publish)
            String nickname = nickname(message);

            chatService.sendChatMessage(ChatMessageDTO.builder()
                    .type(ChatMessage.MessageType.QUIT)
                    .roomId(roomId)
                    .sender(nickname).build(), null);
            // 퇴장한 클라이언트의 roomId 맵핑 정보를 삭제한다.
            chatRoomRedisRepository.removeUserEnterInfo(sessionId);
            log.info("DISCONNECTED {}, {}", sessionId, roomId);
        }

        return message;
    }

    /**
     * 구독자의 oauth2Id.
     *
     * simpUser는 SockJS 핸드셰이크 때 HTTP 세션에서 넘어온 인증 객체다. 클라이언트가
     * 프레임에 실어 보내는 값이 아니라서 위조되지 않는다. CONNECT 헤더의 JWT는
     * 클라이언트가 직접 담는 값이므로 인가 판단의 근거로 쓰지 않는다.
     *
     * OAuth2 로그인에서 {@code Principal.getName()}이 곧 oauth2Id다.
     * CustomOAuth2UserService가 userNameAttributeKey를 oauth2Id와 같은 값으로 맞춰 두었다.
     */
    private String subscriberOauth2Id(Message<?> message) {
        Principal user = (Principal) message.getHeaders().get("simpUser");
        if (user == null) {
            throw new AccessDeniedException("인증되지 않은 구독 요청입니다.");
        }
        return user.getName();
    }

    /**
     * 표시용 대화명.
     *
     * simpUser가 항상 OAuth2AuthenticationToken인 것은 아니다. AccountProfileController가
     * 정보 수정 후 인증 객체를 UsernamePasswordAuthenticationToken으로 갈아끼우기 때문이다
     * (ISS-02). 무조건 캐스팅하면 그 세션의 구독이 ClassCastException으로 죽으므로
     * 타입을 확인하고 안 맞으면 기본값으로 물러난다.
     */
    private String nickname(Message<?> message) {
        Object user = message.getHeaders().get("simpUser");
        if (!(user instanceof OAuth2AuthenticationToken token)) {
            return "UnknownUser";
        }
        return Optional.ofNullable((String) token.getPrincipal().getAttributes().get("nickname"))
                .orElse("UnknownUser");
    }
}
