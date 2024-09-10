package com.eatmate.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountTeamDto {

    private Long account_team_id;
    private Long account_id;
    private Long team_id;
    private boolean is_leader;

    // 팀에 대한 추가 정보 (프로필 개설 팀 리스트 불러오느라...)
    private String team_name;  // 팀 이름
    private String description;  // 팀 설명

}
