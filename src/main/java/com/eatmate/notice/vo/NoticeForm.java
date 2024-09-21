package com.eatmate.notice.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NoticeForm {

    private Long id;
    private String title;
    private String content;
    private String author;

    @Builder
    public NoticeForm(Long id, String title, String content, String author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public void addAuthor(String author) {
        this.author = author;
    }

}
