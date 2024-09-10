package com.eatmate.chat.redisDao;

import com.eatmate.chat.dto.ChatRoomDTO;
import com.eatmate.chat.pubsub.RedisSubscriber;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.AccountTeam;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Repository
public class ChatRoomRedisRepository {


    // Redis
    private static final String CHAT_ROOMS = "CHAT_ROOM";
    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, ChatRoomDTO> opsHashChatRoom;

    @PostConstruct
    private void init() {
        opsHashChatRoom = redisTemplate.opsForHash();
    }
    
    // 모든 채팅방 조회
    public List<ChatRoomDTO> findAllRoom() {
        return opsHashChatRoom.values(CHAT_ROOMS);
    }

    // 특정 채팅방 조회
    public ChatRoomDTO findRoomById(String id) {
        return opsHashChatRoom.get(CHAT_ROOMS, id);
    }

    /**
     * 채팅방 생성 : 서버간 채팅방 공유를 위해 redis hash에 저장한다.
     채팅방의 고유한 roomId가 Redis의 해시 맵에서 채팅방을 식별하는 키로 사용
     chatRoomDTO가 Value로 Redis에 저장
     */
    public ChatRoomDTO createChatRoom(ChatRoomDTO chatRoomDTO) {
        // ChatRoomDTO 사양을 Redis에 저장
        opsHashChatRoom.put(CHAT_ROOMS, chatRoomDTO.getRoomId(), chatRoomDTO);
        return chatRoomDTO;
    }
}

/**
 * 해시 키 : CHAT_ROOMS ( 모든 채팅방이 저장되는 곳)
 * 필드 키 : roomId ( 각 채팅방을 고유하게 식별한s UUID )
 * ChatRoomDto : 해당 채팅방의 정보
 */