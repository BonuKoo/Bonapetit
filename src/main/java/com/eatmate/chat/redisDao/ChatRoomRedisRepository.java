package com.eatmate.chat.redisDao;

import com.eatmate.chat.dto.ChatRoomDTO;
import com.eatmate.chat.pubsub.RedisSubscriber;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.AccountTeam;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class ChatRoomRedisRepository {

    // Redis
    private static final String CHAT_ROOMS = "CHAT_ROOM";
    public static final String USER_COUNT = "USER_COUNT";
    public static final String ENTER_INFO = "ENTER_INFO";

    // CHAT_ROOMS 해시맵에 채팅방을 저장하고 조회
    @Resource(name = "redisTemplate")
    private HashOperations<String, String, ChatRoomDTO> hashOpsChatRoom;
    // 유저 세션 ID와 채팅방 ID를 맵핑
    @Resource(name = "redisTemplate")
    private HashOperations<String, String, String> hashOpsEnterInfo;
    //유저 수 카운트
    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOps;


    // 모든 채팅방 조회
    public List<ChatRoomDTO> findAllRoom() {
        return hashOpsChatRoom.values(CHAT_ROOMS);
    }

    // 특정 채팅방 조회
    public ChatRoomDTO findRoomById(String id) {
        return hashOpsChatRoom.get(CHAT_ROOMS, id);
    }

    /**
     * 채팅방 생성 : 서버간 채팅방 공유를 위해 redis hash에 저장한다.
     채팅방의 고유한 roomId가 Redis의 해시 맵에서 채팅방을 식별하는 키로 사용
     chatRoomDTO가 Value로 Redis에 저장
     */
    public ChatRoomDTO createChatRoom(ChatRoomDTO chatRoomDTO) {
        hashOpsChatRoom.put(CHAT_ROOMS, chatRoomDTO.getRoomId(), chatRoomDTO);
        return chatRoomDTO;
    }

    // 유저가 입장한 채팅방ID와 유저 세션ID 맵핑 정보 저장
    public void setUserEnterInfo(String sessionId, String roomId) {
        hashOpsEnterInfo.put(ENTER_INFO, sessionId, roomId);
    }

    // 유저 세션으로 입장해 있는 채팅방 ID 조회
    public String getUserEnterRoomId(String sessionId) {
        return hashOpsEnterInfo.get(ENTER_INFO, sessionId);
    }

    // 유저 세션정보와 맵핑된 채팅방ID 삭제
    public void removeUserEnterInfo(String sessionId) {
        hashOpsEnterInfo.delete(ENTER_INFO, sessionId);
    }

    // 채팅방 유저수 조회
    public long getUserCount(String roomId) {
        return Long.valueOf(Optional.ofNullable(valueOps.get(USER_COUNT + "_" + roomId)).orElse("0"));
    }

    // 채팅방에 입장한 유저수 +1
    public long plusUserCount(String roomId) {
        return Optional.ofNullable(valueOps.increment(USER_COUNT + "_" + roomId)).orElse(0L);
    }

    // 채팅방에 입장한 유저수 -1
    public long minusUserCount(String roomId) {
        return Optional.ofNullable(valueOps.decrement(USER_COUNT + "_" + roomId)).filter(count -> count > 0).orElse(0L);
    }


}

/**
 * 해시 키 : CHAT_ROOMS ( 모든 채팅방이 저장되는 곳)
 * 필드 키 : roomId ( 각 채팅방을 고유하게 식별한s UUID )
 * ChatRoomDto : 해당 채팅방의 정보
 */
