package com.eatmate.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatMessage {

    public enum MessageType{
        // 입장, 채팅
        ENTER,TALK
    }
    private MessageType type;
    private String roomId;  //방번호
    private String sender;  //메시지 보낸 사람
    private String message; //메시지 내용
}
