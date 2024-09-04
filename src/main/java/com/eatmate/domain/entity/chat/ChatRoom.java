package com.eatmate.domain.entity.chat;

import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoom {

    @Id
    //생성 시 UUID로 랜덤 값을 생성해주자.
    //그냥 Long 으로 해도 뭐.. 문제는 아닐 듯 하다.
    private String roomId;

    private String name;    //채팅방 이름 -> 이 경우 teamName을 등록해줘도 괜찮을 것 같다.

    private Set<WebSocketSession> sessions = new HashSet<>();

    //ChatRoom과 AccountTeam의 관계 또한 다 대 다
    //ChatRoom의 List에 AccountTeam이 들어간다면
    //Team에 속한 놈들이 ChatRoom의 리스트에도 한놈씩 추가 됨
    //추가 되면? sub, pub

    @OneToOne
    private Team team;  //Team 마다 채팅방 하나 씩 서버에 할당

}
