package com.eatmate.dao.repository.chatroom;

import com.eatmate.domain.entity.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom,String> {


    //public ChatRoom findByTeam(Long teamId);

    @Query("SELECT c FROM ChatRoom c WHERE c.team.id = :teamId")
    ChatRoom findByTeam(@Param("teamId") Long teamId);

}
