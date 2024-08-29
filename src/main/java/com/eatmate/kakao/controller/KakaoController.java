package com.eatmate.kakao.controller;

import com.eatmate.kakao.service.KakaoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Map;

@Controller
public class KakaoController {

    @Autowired
    private KakaoService kakaoService;

    // 카카오 로그인
    @GetMapping("/oauth/kakao/callback")
    public String kakaoCallback(@RequestParam String code, Model model, HttpSession session){
        // 인증 코드로 액세스 토근 요청
        String accessToken = kakaoService.getAccessToken(code);
        System.out.println(accessToken);
        // 액세스 토큰을 세션에 저장
        session.setAttribute("kakaoAccessToken", accessToken);
        // 액세스 토큰으로 사용자 정보 요청
        Map<String, Object> userProfile = kakaoService.getUserProfile(accessToken);
        System.out.println("lgoin Controller : "+userProfile);
        // 사용자 정보 모델에 추가
        model.addAttribute("userProfile",userProfile);
        // 뷰 이동
        return "account/userProfile";
    }

    // 카카오 로그아웃
    @GetMapping("/kakao/logout")
    public void logout(HttpServletResponse response, HttpSession session) throws IOException {
        String accessToken = (String) session.getAttribute("kakaoAccessToken");

        if (accessToken != null){
            // 액세스 토큰 무효화
            kakaoService.logout(accessToken);
            session.removeAttribute("kakaoAccessToken");
            session.invalidate();   // 세션 무효화
        }
        // 카카오 로그아웃 URL로 리다이렉트
        String requestUrl = "https://kauth.kakao.com/oauth/logout"
                + "?client_id=4d77a5adb4a0ec1e3c0b6b28c5f48284"
                + "&logout_redirect_uri=http://localhost:8080";     // 카카오 로그아웃에 저장한 url

        response.sendRedirect(requestUrl);
    }

}