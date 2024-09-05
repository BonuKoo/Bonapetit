package com.eatmate.domain.entity.chat;

import com.eatmate.chat.dto.ChatRoomDTO;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoom {

    @Id
    //생성 시 UUID로 랜덤 값을 생성해주자.
    //그냥 Long 으로 해도 뭐.. 문제는 아닐 듯 하다.
    private String roomId;

    private String roomName;    //채팅방 이름 -> 이 경우 teamName을 등록해줘도 괜찮을 것 같다.


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

    //Team에 속한 AcoountTeam List를 가져온다.
    public List<AccountTeam> getTeamMembers(){
        return team.getMembers();
    }

    // Entity -> Redis DTO로 변환
    public ChatRoomDTO toRedisDTO() {
        // AccountTeam 정보를 담을 HashMap 생성
        HashMap<Long, Map<String, Object>> accountTeamInfo = new HashMap<>();
        for (AccountTeam accountTeam : this.getTeam().getMembers()) {
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("accountId", accountTeam.getAccount().getId());
            memberInfo.put("email", accountTeam.getAccount().getEmail());
            memberInfo.put("oauth2id", accountTeam.getAccount().getOauth2id());
            memberInfo.put("isLeader", accountTeam.isLeader());

            accountTeamInfo.put(accountTeam.getId(), memberInfo);
        }

        // DTO 반환 시 accountTeamInfo 포함
        return ChatRoomDTO.builder()
                .roomId(this.roomId)
                .roomName(this.roomName)
                .teamId(this.team.getId())
                .membersInfo(accountTeamInfo)
                .build();
    }

}
