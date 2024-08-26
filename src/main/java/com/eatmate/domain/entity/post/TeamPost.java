package com.eatmate.domain.entity.post;

import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.*;

@Entity
public class TeamPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_post_id")
    private Long id;

    // 작성자
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    // 팀
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    // 조회수
    private int cnt;

}
