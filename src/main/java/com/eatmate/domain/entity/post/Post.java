package com.eatmate.domain.entity.post;

import com.eatmate.domain.entity.user.Account;
import jakarta.persistence.*;

@Entity
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;
    
    //작성자
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;
    
    //조회수
    @Column(name="cnt")
    private int cnt;

}
