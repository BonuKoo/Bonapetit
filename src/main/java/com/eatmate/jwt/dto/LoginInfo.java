package com.eatmate.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LoginInfo {

    private String oauthId;
    private String token;
    private String nickname;

    @Builder
    public LoginInfo(String oauthId, String token, String nickname) {
        this.oauthId = oauthId;
        this.token = token;
        this.nickname = nickname;
    }
}
