package com.eatmate.domain.entity;

import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.*;

@Entity
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account owner;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
}
