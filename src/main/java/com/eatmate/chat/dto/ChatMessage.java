package com.eatmate.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChatMessage {

    private MessageType type; //메시지 타입
    private String roomId;    //방번호
    private String sender;    //메시지 보낸 사람
    private String message;   //메시지 내용
    private long userConnt;   //채팅방 인원수

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public enum MessageType{
        // 입장, 채팅
        ENTER,QUIT,TALK
    }

}
