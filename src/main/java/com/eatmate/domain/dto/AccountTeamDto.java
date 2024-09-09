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

}
