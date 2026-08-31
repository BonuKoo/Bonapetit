package com.eatmate.domain.entity.chat;

import com.eatmate.domain.entity.user.Account;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * 채팅 메시지 영속 엔티티.
 *
 * 기존에는 메시지가 Redis Pub/Sub으로 발행만 되고 어디에도 저장되지 않아,
 * 발행 시점에 구독 중이던 클라이언트만 받고 그대로 사라졌다. 그래서 채팅 내역
 * 조회가 원천적으로 불가능했다.
 *
 * 저장 대상은 TALK 뿐이다. ENTER/QUIT은 실시간 알림으로만 쓰고 저장하지 않는다.
 * 재접속이 잦으면 입퇴장 메시지가 실제 대화보다 많아져 내역 조회에 노이즈가 된다.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "chat_message",
        indexes = @Index(name = "idx_chat_message_room_id", columnList = "room_id, chat_message_id")
)
public class ChatMessage {

    /**
     * 커서 페이징의 정렬 키.
     * createdAt은 같은 밀리초에 여러 건이 들어올 수 있어 커서로 쓰기 부적합하다.
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 발신 계정. 탈퇴 등으로 계정이 사라져도 메시지는 남아야 하므로 nullable이다.
     */
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "account_id")
    private Account sender;

    /**
     * 발신 시점의 닉네임 스냅샷.
     * 닉네임을 바꿔도 과거 메시지의 표시명이 그대로 보존되도록 별도로 들고 있는다.
     */
    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private MessageType type;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(ChatRoom chatRoom, Account sender, String senderName,
                       MessageType type, String message, LocalDateTime createdAt) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.senderName = senderName;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public enum MessageType {
        // 입장, 퇴장, 대화
        ENTER, QUIT, TALK
    }
}
