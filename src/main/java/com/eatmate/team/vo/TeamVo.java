package com.eatmate.team.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamVo {
    // 팀 id
    private Long teamId;
    // 방제
    private String teamName;
    // 주소
    private String addressName;
    // 도로명 주소
    private String roadAddressName;
    // 상호명
    private String placeName;
    // 방장
    private String author;
    // 인원
    private int memCnt;
    // 생성일자
    private String createdDate;
}