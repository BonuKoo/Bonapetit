package com.eatmate.domain.entity.post.form;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor @AllArgsConstructor
public class CreatePostForm {

    private String title;
    private String author;
    private String content;

}
