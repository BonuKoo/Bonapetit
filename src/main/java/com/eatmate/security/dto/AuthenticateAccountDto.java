package com.eatmate.security.dto;

import com.eatmate.domain.entity.user.AccountTeam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticateAccountDto implements Serializable {

    private Long id;

    private String email;

    private String nickname;
    private String password;
    private String roles;

    // AccountTeam은 JPA 엔티티(Serializable 아님)라 Redis 세션 직렬화 대상에서 제외한다.
    // 어디서도 읽히지 않는 필드이므로(getAccountTeams 호출부 없음) transient로도 안전하다.
    private transient List<AccountTeam> accountTeams = new ArrayList<>();

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
