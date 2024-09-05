package com.eatmate.chat.pubsub;

import com.eatmate.chat.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RedisPublisher {

    private final RedisTemplate<String,Object> redisTemplate;

    /**
     *  publish를 통해서, topic (채팅방)에 message가 전달된다.
     */
    public void publish(ChannelTopic topic, ChatMessage message){
        redisTemplate.convertAndSend(topic.getTopic(),message);
    }

}
