package com.eatmate.domain.entity.user;

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



    @Builder
    public Account(Long id, String email, String nickname, String password, List<AccountTeam> accountTeams) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.accountTeams = accountTeams;
    }

}