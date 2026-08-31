package com.eatmate.notice.vo;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

import java.io.Serializable;

@Data
public class NoticePageForm implements Serializable {

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
