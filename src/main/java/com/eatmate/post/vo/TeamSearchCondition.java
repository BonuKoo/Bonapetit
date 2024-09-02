package com.eatmate.post.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TeamSearchCondition {

    //작성자 닉네임
    private String author;
    //팀 이름
    private String teamName;
    //태그 정보
    private String tag;
    // 검색 날짜
    private LocalDate searchDate;
    // 지역
    private String location;
}
