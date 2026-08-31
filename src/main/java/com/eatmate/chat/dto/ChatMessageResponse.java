package com.eatmate.chat.dto;

import com.eatmate.domain.entity.chat.ChatMessage;

import java.time.LocalDateTime;

/**
 * 채팅 내역 조회 응답의 개별 메시지.
 *
 * sender는 발신 시점 닉네임 스냅샷(senderName)이다. 계정의 현재 닉네임이 아니라
 * 그때 보낸 이름을 그대로 보여준다.
 *
 * id를 함께 내려주는 이유는 두 가지다. 프론트의 v-for :key로 쓰이고
 * (지금은 실시간 메시지에 id가 없어 전부 undefined다), 커서 페이징의 커서가 된다.
 */
public record ChatMessageResponse(
        Long id,
        String sender,
        String message,
        ChatMessage.MessageType type,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSenderName(),
                message.getMessage(),
                message.getType(),
                message.getCreatedAt()
        );
    }
}
