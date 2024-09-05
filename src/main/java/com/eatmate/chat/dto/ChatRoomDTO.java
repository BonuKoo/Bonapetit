package com.eatmate.chat.dto;

import com.eatmate.domain.entity.chat.ChatRoom;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ChatRoomDTO implements Serializable {

    //Redis에 저장되는 객체들은
    //Serialize 가능해야 하므로, serialVersionUID를 세팅해준다. -> 공부 필요

    private static final long serialVersionUID = 234709823748L;

    private String roomId;
    private String roomName;

    @Builder
    public ChatRoomDTO(String roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
    }

    public static ChatRoomDTO create(String roomId,String roomName){
        return ChatRoomDTO.builder()
                .roomId(roomId)
                .roomName(roomName)
                .build();
    }
}
