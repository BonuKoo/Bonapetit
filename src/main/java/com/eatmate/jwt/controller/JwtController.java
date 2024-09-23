package com.eatmate.jwt.controller;

import com.eatmate.jwt.JwtTokenProvider;
import com.eatmate.jwt.dto.LoginInfo;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class JwtController {

    private final JwtTokenProvider jwtTokenProvider;
    
    //JWT 토큰 생성하는 예시
    @GetMapping(value = "/info", produces = "application/json")
    @ResponseBody
    public LoginInfo getUserInfo(){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        log.info("auth :{}",auth.toString());

        //oauthId
        String oauthId = auth.getName();
        log.info("oauthId :{}",oauthId);
        //로그인 중인 계정의 닉네임
        String nickname = null;

        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getSession();
        String platform = (String) session.getAttribute("provider");

        switch (platform) {
            case "kakao":
                nickname = (String) ((Map<String, Object>) ((OAuth2AuthenticationToken) auth).getPrincipal()
                        .getAttributes()
                        .get("properties"))
                        .get("nickname");
                break;
            case "naver":
                OAuth2User test = ((OAuth2AuthenticationToken) auth).getPrincipal();
                System.out.println(test);
                break;
            case "google":
                nickname = (String) ((OAuth2AuthenticationToken) auth).getPrincipal()
                        .getAttributes()
                        .get("name");
                break;
        }


        log.info("nickname :{}",nickname);

        return LoginInfo.builder()
                .oauthId(oauthId)
                .nickname(nickname)
                .token(jwtTokenProvider.generateToken(oauthId,nickname))
                .build();

    }

}