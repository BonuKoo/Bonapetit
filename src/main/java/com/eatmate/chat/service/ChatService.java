package com.eatmate.chat.service;


import com.eatmate.chat.dto.ChatMessageDTO;
import com.eatmate.domain.entity.chat.ChatMessage;

import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chat.ChatMessageRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.domain.entity.user.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ChatService {

    private final ChannelTopic channelTopic;
    private final RedisTemplate redisTemplate;
    private final ChatRoomRedisRepository chatRoomRedisRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final AccountRepository accountRepository;

    /**
     * destination정보에서 roomId 추출
     */
    public String getRoomId(String destination) {
        int lastIndex = destination.lastIndexOf('/');
        if (lastIndex != -1)
            return destination.substring(lastIndex + 1);
        else
            return "";
    }

    /**
     * 채팅방에 메시지 발송. TALK이면 발송 전에 RDBMS에 영속화한다.
     *
     * 저장을 여기(publish 지점)에서 하는 이유: RedisSubscriber에 두면 Pub/Sub이
     * 구독 중인 모든 서버 인스턴스에 전달되므로 서버 N대에서 같은 메시지가 N번
     * 저장된다. publish 지점은 메시지당 정확히 1회 실행된다.
     *
     * 저장이 실패하면 예외가 전파되어 publish도 되지 않는다(정합성 우선).
     * 가용성을 택하려면 save를 try-catch로 감싸 실패해도 publish를 진행하면 되지만,
     * 그 경우 화면에는 보이는데 내역에는 없는 메시지가 생긴다.
     *
     * @param senderOauth2Id 발신 계정 식별자. ENTER/QUIT처럼 시스템이 만든 메시지는
     *                       null이며 어차피 저장하지 않는다.
     */
    @Transactional
    public void sendChatMessage(ChatMessageDTO chatMessage, String senderOauth2Id) {
        chatMessage.setUserCount(chatRoomRedisRepository.getUserCount(chatMessage.getRoomId()));

        if (ChatMessage.MessageType.ENTER.equals(chatMessage.getType())) {
            chatMessage.setMessage(chatMessage.getSender() + "님이 방에 입장했습니다.");
            chatMessage.setSender("[알림]");
        } else if (ChatMessage.MessageType.QUIT.equals(chatMessage.getType())) {
            chatMessage.setMessage(chatMessage.getSender() + "님이 방에서 나갔습니다.");
            chatMessage.setSender("[알림]");
        } else {
            // TALK만 저장한다. 입퇴장 알림은 재접속이 잦으면 실제 대화보다 많아져
            // 내역 조회에 노이즈가 된다.
            persist(chatMessage, senderOauth2Id);
        }

        redisTemplate.convertAndSend(channelTopic.getTopic(), chatMessage);
    }

    private void persist(ChatMessageDTO dto, String senderOauth2Id) {
        Account sender = (senderOauth2Id == null)
                ? null
                : accountRepository.findByOauth2id(senderOauth2Id).orElse(null);

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                // getReferenceById는 프록시만 만들어 메시지당 SELECT 1회를 아낀다.
                // 존재하지 않는 방이면 INSERT 시 FK 제약으로 걸러진다.
                .chatRoom(chatRoomRepository.getReferenceById(dto.getRoomId()))
                .sender(sender)
                .senderName(dto.getSender())
                .type(ChatMessage.MessageType.TALK)
                .message(dto.getMessage())
                .build());

        // 발행되는 DTO에도 id를 실어 보낸다. 프론트가 실시간 수신분과 조회한
        // 내역을 같은 키로 다룰 수 있어야 중복 렌더링을 막을 수 있다.
        dto.setId(saved.getId());
    }
}
