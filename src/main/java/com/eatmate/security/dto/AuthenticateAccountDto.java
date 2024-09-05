package com.eatmate.security.dto;

import com.eatmate.domain.entity.post.TeamPost;
import com.eatmate.domain.entity.user.AccountRole;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.global.domain.UploadFileOfAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticateAccountDto {

    private Long id;

    private String email;

    private String nickname;
    private String password;
    private String roles;
    private List<AccountTeam> accountTeams = new ArrayList<>();
    private List<AccountRole> accountRoles = new ArrayList<>();
    private List<TeamPost> teamPosts = new ArrayList<>();
    private List<UploadFileOfAccount> files = new ArrayList<>();

    @Builder
    public AuthenticateAccountDto(List<UploadFileOfAccount> files, List<TeamPost> teamPosts, List<AccountRole> accountRoles, List<AccountTeam> accountTeams, String roles, String password, String nickname, String email, Long id) {
        this.files = files;
        this.teamPosts = teamPosts;
        this.accountRoles = accountRoles;
        this.accountTeams = accountTeams;
        this.roles = roles;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.id = id;
    }
}
