package com.eatmate.dao.repository.chatroom;

import com.eatmate.domain.entity.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom,String> {
}
