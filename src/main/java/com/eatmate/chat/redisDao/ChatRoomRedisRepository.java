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

    // 채팅방(topic)에 발행되는 메시지를 처리할 Listner
    private final RedisMessageListenerContainer redisMessageListener;
    // 구독 처리 서비스
    private final RedisSubscriber redisSubscriber;
    // Redis
    private static final String CHAT_ROOMS = "CHAT_ROOM";
    private final RedisTemplate<String, Object> redisTemplate;

    private HashOperations<String, String, ChatRoomDTO> opsHashChatRoom;

    // 채팅방의 대화 메시지를 발행하기 위한 redis topic 정보. 서버별로 채팅방에 매치되는 topic정보를 Map에 넣어 roomId로 찾을수 있도록 한다.
    private Map<String, ChannelTopic> topics;

    @PostConstruct
    private void init() {

        opsHashChatRoom = redisTemplate.opsForHash();
        topics = new HashMap<>();

    }

    public List<ChatRoomDTO> findAllRoom() {
        return opsHashChatRoom.values(CHAT_ROOMS);
    }

    public ChatRoomDTO findRoomById(String id) {
        return opsHashChatRoom.get(CHAT_ROOMS, id);
    }

    /**
     * 채팅방 생성 : 서버간 채팅방 공유를 위해 redis hash에 저장한다.
     */

    public ChatRoomDTO createChatRoom(ChatRoomDTO chatRoomDTO, AccountTeam creator) {

        // ChatRoomDTO에 생성자의 정보를 추가
        HashMap<Long, Map<String, Object>> membersInfo = new HashMap<>();

        // 생성자의 정보를 첫 번째 멤버로 추가
        Map<String, Object> creatorInfo = new HashMap<>();
        creatorInfo.put("accountId", creator.getAccount().getId());
        creatorInfo.put("email", creator.getAccount().getEmail());
        creatorInfo.put("oauth2id", creator.getAccount().getOauth2id());
        creatorInfo.put("isLeader", creator.isLeader());

        membersInfo.put(creator.getId(), creatorInfo);

        // chatRoomDTO에 멤버 정보를 설정
        chatRoomDTO.setMembersInfo(membersInfo);

        // ChatRoomDTO를 Redis에 저장
        opsHashChatRoom.put(CHAT_ROOMS, chatRoomDTO.getRoomId(), chatRoomDTO);

        // 자동으로 채팅방에 입장
        enterChatRoom(chatRoomDTO.getRoomId());

        return chatRoomDTO;
    }

    /*
    // Register members to the chat room
    public void registerMembersToChatRoom(String roomId, List<AccountTeam> members) {
        for (AccountTeam member : members) {
            String memberId = member.getAccount().getId().toString();
            HashMap<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("nickname", member.getAccount().getNickname());
            memberInfo.put("isLeader", member.isLeader());
            // Store member info in Redis under the chat room
            opsHashMembers.put(roomId + ":members", memberId, memberInfo);
        }
    }
    */

    // Enter chat room (set up Redis topic)
    public void enterChatRoom(String roomId) {
        ChannelTopic topic = topics.get(roomId);

        if (topic == null) {
            topic = new ChannelTopic(roomId);
            redisMessageListener.addMessageListener(redisSubscriber, topic);
            topics.put(roomId, topic);
        }
    }

    // Get topic by roomId
    public ChannelTopic getTopic(String roomId) {
        return topics.get(roomId);
    }
}