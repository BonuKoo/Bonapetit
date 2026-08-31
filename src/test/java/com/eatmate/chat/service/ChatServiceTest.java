package com.eatmate.chat.service;

import com.eatmate.chat.dto.ChatMessageDTO;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chat.ChatMessageRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChannelTopic channelTopic;
    @Mock private RedisTemplate redisTemplate;
    @Mock private ChatRoomRedisRepository chatRoomRedisRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private AccountRepository accountRepository;

    @InjectMocks private ChatService chatService;

    private ChatMessageDTO dto(ChatMessage.MessageType type) {
        return ChatMessageDTO.builder()
                .type(type)
                .roomId("room-1")
                .sender("테스터")
                .message("안녕하세요")
                .build();
    }

    @Test
    @DisplayName("TALK은 저장하고 발행한다")
    void talk_isPersistedAndPublished() {
        given(channelTopic.getTopic()).willReturn("chatroom");
        given(accountRepository.findByOauth2id("oauth-1"))
                .willReturn(Optional.of(Account.builder().nickname("테스터").build()));
        given(chatRoomRepository.getReferenceById("room-1"))
                .willReturn(ChatRoom.builder().roomId("room-1").build());

        chatService.sendChatMessage(dto(ChatMessage.MessageType.TALK), "oauth-1");

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(saved.capture());
        assertThat(saved.getValue().getMessage()).isEqualTo("안녕하세요");
        assertThat(saved.getValue().getType()).isEqualTo(ChatMessage.MessageType.TALK);
        // 발신 시점 닉네임이 스냅샷으로 남아야 한다.
        assertThat(saved.getValue().getSenderName()).isEqualTo("테스터");

        verify(redisTemplate).convertAndSend(eq("chatroom"), any(ChatMessageDTO.class));
    }

    @Test
    @DisplayName("ENTER는 저장하지 않고 발행만 한다")
    void enter_isPublishedButNotPersisted() {
        given(channelTopic.getTopic()).willReturn("chatroom");

        ChatMessageDTO message = dto(ChatMessage.MessageType.ENTER);
        chatService.sendChatMessage(message, null);

        verify(chatMessageRepository, never()).save(any());
        verify(redisTemplate).convertAndSend(eq("chatroom"), any(ChatMessageDTO.class));
        assertThat(message.getSender()).isEqualTo("[알림]");
        assertThat(message.getMessage()).isEqualTo("테스터님이 방에 입장했습니다.");
    }

    @Test
    @DisplayName("QUIT은 저장하지 않고 발행만 한다")
    void quit_isPublishedButNotPersisted() {
        given(channelTopic.getTopic()).willReturn("chatroom");

        ChatMessageDTO message = dto(ChatMessage.MessageType.QUIT);
        chatService.sendChatMessage(message, null);

        verify(chatMessageRepository, never()).save(any());
        verify(redisTemplate).convertAndSend(eq("chatroom"), any(ChatMessageDTO.class));
        assertThat(message.getSender()).isEqualTo("[알림]");
        assertThat(message.getMessage()).isEqualTo("테스터님이 방에서 나갔습니다.");
    }

    @Test
    @DisplayName("저장이 실패하면 발행하지 않는다")
    void publishIsSkippedWhenPersistFails() {
        given(accountRepository.findByOauth2id(anyString())).willReturn(Optional.empty());
        given(chatRoomRepository.getReferenceById("room-1"))
                .willReturn(ChatRoom.builder().roomId("room-1").build());
        given(chatMessageRepository.save(any()))
                .willThrow(new RuntimeException("DB 장애"));

        assertThatThrownBy(() -> chatService.sendChatMessage(dto(ChatMessage.MessageType.TALK), "oauth-1"))
                .isInstanceOf(RuntimeException.class);

        verify(redisTemplate, never()).convertAndSend(anyString(), any());
    }

    @Test
    @DisplayName("계정을 찾지 못해도 메시지는 저장된다")
    void unknownSenderStillPersistsMessage() {
        given(channelTopic.getTopic()).willReturn("chatroom");
        given(accountRepository.findByOauth2id(anyString())).willReturn(Optional.empty());
        given(chatRoomRepository.getReferenceById("room-1"))
                .willReturn(ChatRoom.builder().roomId("room-1").build());

        chatService.sendChatMessage(dto(ChatMessage.MessageType.TALK), "oauth-unknown");

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(saved.capture());
        assertThat(saved.getValue().getSender()).isNull();
        assertThat(saved.getValue().getSenderName()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("메시지당 getUserCount는 한 번만 호출된다")
    void userCountIsFetchedOncePerMessage() {
        given(channelTopic.getTopic()).willReturn("chatroom");

        chatService.sendChatMessage(dto(ChatMessage.MessageType.ENTER), null);

        // 이전엔 ChatController와 ChatService 양쪽에서 호출해 Redis 왕복이
        // 메시지당 1회 낭비되고 있었다. 컨트롤러 쪽 호출을 제거했다.
        verify(chatRoomRedisRepository, times(1)).getUserCount("room-1");
    }
}
