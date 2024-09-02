package com.eatmate.post.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostForm {
    
    //방제
    private String title;
    //간단한 설명
    private String description;
    //방장 닉네임
    private String author;

    //팀 이름
    private String teamName;

    @Builder
    public PostForm(String title, String description, String author, String teamName) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.teamName = teamName;
    }

    public void addAuthor(String author) {
        this.author = author;
    }

}
