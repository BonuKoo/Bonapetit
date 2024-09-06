package com.eatmate.weblogout.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class LoginController {

    // 로그인 성공 시 세션에 사용자 OAuth2 ID 저장
    @GetMapping("/login/success")
    public String loginSuccess(HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            String oauth2Id = (String) response.get("id");

            // 세션에 OAuth2 ID 저장
            session.setAttribute("loggedInOauth2Id", oauth2Id);
        }
        return "redirect:/";
    }
}
