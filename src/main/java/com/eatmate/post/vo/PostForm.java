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

    //해시 태그 -> 추후 리스트로 변경
    //private String tag;

    @Builder
    public PostForm(String description, String author, String teamName
            //,String tag
    ) {
        this.description = description;
        this.author = author;
        this.teamName = teamName;
        //this.tag = tag;
    }

    public void addAuthor(String author) {
        this.author = author;
    }
}
