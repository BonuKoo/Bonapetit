package com.eatmate.chat.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class WebSocketChatHandler extends TextWebSocketHandler {


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        String payload = message.getPayload();
        log.info("payload {}",payload);
        TextMessage textMessage = new TextMessage("님이 입장하셨습니다.");
        session.sendMessage(textMessage);
    }
}

/**
    웹소켓 클라이언트로부터 채팅 메시지를 전달받아서 채팅 메시지 객체로 변환
    전달받은 메시지에 담긴 채팅방 ID로 발송 대상 채팅방 정보를 조회
    해당 채팅방에 입장해있는 모든 클라이언트들에게 타입에 따른 메시지 발송
*/