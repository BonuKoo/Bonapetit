package com.eatmate.chat.handler;

import com.eatmate.chat.dto.ChatMessageDTO;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.chat.service.ChatRoomMembershipVerifier;
import com.eatmate.chat.service.ChatService;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * STOMP 구독 인가.
 *
 * 구독은 그 방의 대화를 실시간으로 받겠다는 뜻이므로 내역 조회와 같은 검증이 필요하다.
 * 이게 없으면 roomId만 알면 남의 대화를 열람할 수 있다(ISS-01).
 */
@ExtendWith(MockitoExtension.class)
class StompHandlerTest {

    private static final String ROOM_ID = "room-1";
    private static final String OAUTH_ID = "oauth-1";
    private static final String DESTINATION = "/sub/chat/room/" + ROOM_ID;

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private ChatRoomRedisRepository chatRoomRedisRepository;
    @Mock private ChatService chatService;
    @Mock private ChatRoomMembershipVerifier membershipVerifier;

    @InjectMocks private StompHandler stompHandler;

    private Message<byte[]> frame(StompCommand command, String destination, String oauth2Id) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (oauth2Id != null) {
            accessor.setUser((Principal) () -> oauth2Id);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("멤버의 구독은 방 진입 처리까지 진행된다")
    void memberSubscriptionEntersRoom() {
        stompHandler.preSend(frame(StompCommand.SUBSCRIBE, DESTINATION, OAUTH_ID), null);

        verify(membershipVerifier).verify(ROOM_ID, OAUTH_ID);
        verify(chatRoomRedisRepository).setUserEnterInfo("session-1", ROOM_ID);
        verify(chatRoomRedisRepository).plusUserCount(ROOM_ID);

        ArgumentCaptor<ChatMessageDTO> captor = ArgumentCaptor.forClass(ChatMessageDTO.class);
        verify(chatService).sendChatMessage(captor.capture(), any());
        assertThat(captor.getValue().getType()).isEqualTo(ChatMessage.MessageType.ENTER);
        assertThat(captor.getValue().getRoomId()).isEqualTo(ROOM_ID);
    }

    @Test
    @DisplayName("멤버가 아니면 구독이 거부되고 방에 들어가지 않는다")
    void nonMemberSubscriptionIsRejected() {
        willThrow(new AccessDeniedException("이 채팅방의 멤버가 아닙니다."))
                .given(membershipVerifier).verify(ROOM_ID, OAUTH_ID);

        assertThatThrownBy(() ->
                stompHandler.preSend(frame(StompCommand.SUBSCRIBE, DESTINATION, OAUTH_ID), null))
                .isInstanceOf(AccessDeniedException.class);

        // 인원수 증가·입장 알림 같은 부수효과가 하나도 일어나면 안 된다.
        verifyNoInteractions(chatRoomRedisRepository);
        verify(chatService, never()).sendChatMessage(any(), any());
    }

    @Test
    @DisplayName("인증 주체가 없는 구독은 검증에 가기도 전에 거부된다")
    void unauthenticatedSubscriptionIsRejected() {
        assertThatThrownBy(() ->
                stompHandler.preSend(frame(StompCommand.SUBSCRIBE, DESTINATION, null), null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(membershipVerifier, chatRoomRedisRepository);
    }

    @Test
    @DisplayName("채팅방이 아닌 destination은 방 진입 처리를 하지 않는다")
    void nonChatDestinationIsPassedThrough() {
        // 마지막 경로 조각을 roomId로 삼던 방식은 "/sub/무엇이든"에도 걸렸다.
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, "/sub/something/else", OAUTH_ID);

        assertThat(stompHandler.preSend(message, null)).isSameAs(message);

        verifyNoInteractions(membershipVerifier, chatRoomRedisRepository);
        verify(chatService, never()).sendChatMessage(any(), any());
    }

    @Test
    @DisplayName("구독한 적 없는 세션의 연결 종료는 퇴장 처리를 하지 않는다")
    void disconnectWithoutSubscriptionDoesNothing() {
        // 구독이 거부되면 ENTER_INFO에 매핑이 남지 않는다. 그때 roomId는 null이다.
        given(chatRoomRedisRepository.getUserEnterRoomId("session-1")).willReturn(null);

        stompHandler.preSend(frame(StompCommand.DISCONNECT, null, OAUTH_ID), null);

        verify(chatRoomRedisRepository, never()).minusUserCount(anyString());
        verify(chatService, never()).sendChatMessage(any(), any());
    }

    @Test
    @DisplayName("CONNECT는 토큰만 검증한다")
    void connectValidatesToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("token", "jwt-token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatCode(() -> stompHandler.preSend(message, null)).doesNotThrowAnyException();

        verify(jwtTokenProvider).validateToken("jwt-token");
        verifyNoInteractions(membershipVerifier);
    }
}
