package com.eatmate.security.dto;

import com.eatmate.domain.entity.user.AccountTeam;
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

    @Builder
    public AuthenticateAccountDto(List<AccountTeam> accountTeams, String roles, String password, String nickname, String email, Long id) {
        this.accountTeams = accountTeams;
        this.roles = roles;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.id = id;
    }
}
