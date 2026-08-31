package com.eatmate.chat.dto;

import com.eatmate.domain.entity.chat.ChatMessage;
import lombok.*;

/**
 * WebSocket/Redis Pub/Sub으로 오가는 실시간 메시지 전송 객체.
 *
 * MessageType은 도메인 개념이므로 엔티티(ChatMessage)가 소유하고 여기서 참조한다.
 */
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChatMessageDTO {

    /**
     * 저장된 메시지의 id. TALK을 영속화한 뒤 채워진다(ENTER/QUIT은 저장하지 않으므로 null).
     * 프론트의 v-for :key로 쓰이고, 실시간 수신분과 조회한 내역의 중복 판별에도 쓴다.
     */
    private Long id;

    private ChatMessage.MessageType type; //메시지 타입
    private String roomId;    //방번호
    private String sender;    //메시지 보낸 사람
    private String message;   //메시지 내용
    private long userCount;   //채팅방 인원수
}
