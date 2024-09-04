package com.eatmate.chat.service;

import com.eatmate.chat.dto.ChatRoom;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    private final ObjectMapper objectMapper;
    private Map<String, ChatRoom> chatRooms;

    @PostConstruct
    private void init() {
        chatRooms = new LinkedHashMap<>();
    }

    public List<ChatRoom> findAllRoom() {
        return new ArrayList<>(chatRooms.values());
    }

    public ChatRoom findRoomById(String roomId) {
        return chatRooms.get(roomId);
    }

    public ChatRoom createRoom(String name) {
        String randomId = UUID.randomUUID().toString();
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(randomId)
                .name(name)
                .build();
        chatRooms.put(randomId, chatRoom);
        return chatRoom;
    }

    public <T> void sendMessage(WebSocketSession session, T message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}

/**

    채팅방을 생성, 조회하고 하나의 세션에 메시지 발송을 하는 서비스 구현.

    채팅방 Map은 서버에 생성된 모든 채팅방의 정보를 모아둔 구조체.
    정보 저장은 우선 HasnHap에 저장

    채팅장 조회 - 채팅방 Map에 담긴 정보를 조회
    채팅방 생성 - Random UUID로 구별 ID를 가진 채팅방 객체를 생성하고
    채팅방 Map에 추가
    메시지 발송 - 지정한 WebSocket 세션에 메시지를 발송.

 */