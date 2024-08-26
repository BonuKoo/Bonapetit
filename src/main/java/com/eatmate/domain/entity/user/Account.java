package com.eatmate.domain.entity.user;

import com.eatmate.domain.entity.post.Post;
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
    private String email;    // 로그인은 email로

    @Column(name = "nick_name")
    private String nickname;

    @Column(name = "password", nullable = false)
    private String password;

    /*==Team==*/
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<AccountTeam> accountTeams = new ArrayList<>();

    /*==Role==*/
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<AccountRole> accountRoles = new ArrayList<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();
}