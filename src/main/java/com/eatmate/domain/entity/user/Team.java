package com.eatmate.domain.entity.user;

import com.eatmate.domain.entity.post.Post;
import com.eatmate.domain.entity.post.TeamPost;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "team_name")
    private String teamName;

    //방장
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account_id;

    @OneToMany(mappedBy = "team")
    private List<AccountTeam> members = new ArrayList<>();

    @OneToMany(mappedBy = "team")
    private List<TeamPost> teamPosts = new ArrayList<>();  // 팀과 팀 게시글 간의 관계

}
