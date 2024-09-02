package com.eatmate.post.vo;

import com.querydsl.core.annotations.QueryProjection;

public class PostPageDto {

    private Long id;
    private String title;
    private String teamName;

    private String leaderName;
    private String createdDate;

    private String tagName;
    //private String location;

    private int memberListSize;

    @QueryProjection
    public PostPageDto(Long id, String title, String teamName, String leaderName, String createdDate,String tagName) {
        this.id = id;
        this.title = title;
        this.teamName = teamName;
        this.leaderName = leaderName;
        this.createdDate = createdDate;
        this.tagName = tagName;
    }
}
