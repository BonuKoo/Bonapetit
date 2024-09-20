package com.eatmate.notice.vo;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class NoticePageForm {

    private Long id;
    private String title;
    private String author;
    private String content;

    @QueryProjection
    public NoticePageForm(Long id, String title, String author, String content) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.content = content;
    }
}
