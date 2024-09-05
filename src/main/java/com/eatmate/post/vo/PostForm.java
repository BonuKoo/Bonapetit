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
    // 지역
    private String location;

    //해시 태그 -> 추후 리스트로 변경
    //private String tag;

    @Builder
    public PostForm(String description, String author, String teamName, String location
            //,String tag
    ) {
        this.description = description;
        this.author = author;
        this.teamName = teamName;
        this.location = location;
        //this.tag = tag;
    }

    public void addAuthor(String author) {
        this.author = author;
    }
}
