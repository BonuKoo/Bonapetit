package com.eatmate.post.vo;

import com.querydsl.core.annotations.QueryProjection;

import java.time.LocalDateTime;

public class PostPageDto {

    private Long id;
    private String title;
    private String teamName;
    private String leaderName;
    private LocalDateTime createdDate;  // 수정된 부분

    @QueryProjection
    public PostPageDto(Long id, String title, String teamName, String leaderName, LocalDateTime createdDate) {  // 수정된 부분
        this.id = id;
        this.title = title;
        this.teamName = teamName;
        this.leaderName = leaderName;
        this.createdDate = createdDate;
    }

}
