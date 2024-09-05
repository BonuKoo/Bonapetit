package com.eatmate.post.vo;

import com.querydsl.core.annotations.QueryProjection;

import java.time.LocalDateTime;

public class PostPageDto {

    private Long id;
    private String teamName;
    private LocalDateTime createdDate;

    @QueryProjection
    public PostPageDto(Long id, String teamName, LocalDateTime createdDate) {  // 수정된 부분
        this.id = id;
        this.teamName = teamName;
        this.createdDate = createdDate;
    }

}
