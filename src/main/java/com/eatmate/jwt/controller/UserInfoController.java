package com.eatmate.jwt.controller;

import com.eatmate.jwt.JwtTokenProvider;
import com.eatmate.jwt.dto.LoginInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserInfoController {

    private final JwtTokenProvider jwtTokenProvider;
    
    //JWT 토큰 생성하는 예시
    @GetMapping("/info")
    @ResponseBody
    public LoginInfo getUserInfo(){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //oauthId
        String oauthId = auth.getName();
        //로그인 중인 계정의 닉네임
        String nickname = (String) ((Map<String, Object>) ((OAuth2AuthenticationToken) auth).getPrincipal().getAttributes().get("properties")).get("nickname");

        return LoginInfo.builder()
                .nickname(nickname)
                .token(jwtTokenProvider.generateToken(nickname,nickname))
                .build();
    }
    
}
