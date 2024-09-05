package com.eatmate.chat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
public class ChatRoomDTO implements Serializable {

    //Redis에 저장되는 객체들은
    //Serialize 가능해야 하므로, serialVersionUID를 세팅해준다. -> 공부 필요

    private static final long serialVersionUID = 234709823748L;

    private String roomId;
    private String roomName;
    private Long teamId;

    private HashMap<Long, Map<String, Object>> membersInfo;

    @Builder
    public ChatRoomDTO(String roomId, String roomName, Long teamId, HashMap<Long, Map<String, Object>> membersInfo) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.teamId = teamId;
        this.membersInfo = membersInfo;
    }




}
