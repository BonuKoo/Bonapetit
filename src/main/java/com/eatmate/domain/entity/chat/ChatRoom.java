package com.eatmate.domain.entity.chat;

import com.eatmate.chat.dto.ChatRoomDTO;
import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoom {

    @Id
    //생성 시 UUID로 랜덤 값을 생성해주자.
    //그냥 Long 으로 해도 뭐.. 문제는 아닐 듯 하다.
    private String roomId;

    private String roomName;    //채팅방 이름 -> 이 경우 teamName을 등록해줘도 괜찮을 것 같다.

    /*
    // TODO : Team에 속한 AccountTeam (회원) 들을 ChatRoom으로

   근데, 다이나믹 Array로 구현할 필요가 있을 지는 생각해보자.
   배열 길이 값은 어차피 5가 끝인데 굳이 다이나믹 어레이를 ?

    //ChatRoom과 AccountTeam의 관계 또한 다 대 다
    //ChatRoom의 List에 AccountTeam이 들어간다면
    //Team에 속한 놈들이 ChatRoom의 리스트에도 한놈씩 추가 됨
    //추가 되면? sub, pub


    private List<AccountTeam> list = new ArrayList<>();
    */


    @OneToOne
    @JoinColumn(name = "team_id")
    private Team team;  //Team 마다 채팅방 하나 씩 서버에 할당

    @Builder
    public ChatRoom(String roomId, String roomName, Team team) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.team = team;
    }

    public void setTeam(Team team) {
        this.team = team;
        if (team != null && team.getChatRoom() != this) {
            team.setChatRoom(this); // 이미 관계가 설정되어 있지 않다면 설정
        }
    }

    // Entity -> Redis DTO로 변환
    public ChatRoomDTO toRedisDTO(){
        return ChatRoomDTO.builder()
                .roomId(this.roomId)
                .roomName(this.roomName)
                .build();
    }

}
