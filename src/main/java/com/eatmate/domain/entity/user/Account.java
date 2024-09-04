package com.eatmate.domain.entity.user;

import com.eatmate.domain.entity.post.Post;
import com.eatmate.domain.entity.post.TeamPost;
import com.eatmate.global.domain.UploadFileOfAccount;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@ToString
public class Account {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "nick_name")
    private String nickname;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "roles")
    private String roles;

    @Column(name = "oauth2_id", unique = true)
    private String oauth2id;

    @Column(name = "access_token")
    private String accesstoken;

    @Column(name = "provider")
    private String provider;

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

    @OneToMany(mappedBy = "account", cascade = CascadeType.MERGE, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<UploadFileOfAccount> files = new ArrayList<>();

    @Builder
    public Account(Long id, String email, String nickname, String password, List<AccountTeam> accountTeams, List<AccountRole> accountRoles, List<Post> posts, List<TeamPost> teamPosts, List<UploadFileOfAccount> files) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.accountTeams = accountTeams;
        this.accountRoles = accountRoles;
        this.posts = posts;
        this.teamPosts = teamPosts;
        this.files = (files != null) ? files : new ArrayList<>();
    }

    /*
       File 연관
    */
    public void addFile(UploadFileOfAccount file) {
        files.add(file);
        file.attachAccount(this);
    }

    public void removeFile(UploadFileOfAccount file) {
        files.remove(file);
        file.attachAccount(null);
    }
}