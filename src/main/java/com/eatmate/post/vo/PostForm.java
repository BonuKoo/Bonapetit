package com.eatmate.post.vo;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
public class PostForm {
    
    //팀 이름
    private String teamName;
    //간단한 설명
    private String description;
    //방장 닉네임
    private String author;

    @Builder
    public PostForm(String description, String author, String teamName
    ) {
        this.description = description;
        this.author = author;
        this.teamName = teamName;
    }

    public void addAuthor(String author) {
        this.author = author;
    }
}
