package com.eatmate.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TeamDto {

    private Long team_id;
    private String team_name;
    private String description;
//    private String map_id;
//    private String address_name;
//    private String phone;
//    private String place_name;
//    private String place_url;
//    private String road_address_name;
//    private String x;
//    private String y;
}
