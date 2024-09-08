package com.eatmate.team.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamForm {
   private Long teamId;
   private String userName;
   private String roomId;

   @Builder
   public TeamForm(Long teamId, String userName,String roomId) {
      this.teamId = teamId;
      this.userName = userName;
      this.roomId = roomId;
   }

   public void joinRoomId(String roomId) {
      this.roomId = roomId;
   }
}
