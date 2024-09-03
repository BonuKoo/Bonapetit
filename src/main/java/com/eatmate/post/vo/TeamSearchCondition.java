package com.eatmate.post.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TeamSearchCondition {

    // 1. 지역
    private String location;
    // 2. 작성자 닉네임
    private String author;
    // 3. 팀 이름
    private String teamName;
    // 4. 태그 정보
    private String tag;
}
