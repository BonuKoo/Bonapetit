package com.eatmate.domain.entity.user;

import com.eatmate.domain.entity.post.Post;
import com.eatmate.domain.entity.post.TeamPost;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Account {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "nick_name")
    private String nickname;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(unique = true)
    private String oauth2_id;

    @Column
    private String access_token;

    /*==Team==*/
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<AccountTeam> accountTeams = new ArrayList<>();

    /*==Role==*/
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<AccountRole> accountRoles = new ArrayList<>();

    // 사용자가 작성한 일반 게시글
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();

    // 사용자가 작성한 팀 게시글
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<TeamPost> teamPosts = new ArrayList<>();
}