package com.eatmate.chat.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChatMessage {

    private MessageType type; //메시지 타입
    private String roomId;    //방번호
    private String sender;    //메시지 보낸 사람
    private String message;   //메시지 내용
    private long userConnt;   //채팅방 인원수

    public enum MessageType{
        // 입장, 채팅
        ENTER,QUIT,TALK
    }

}
