package com.eatmate.weblogout.controller;

import com.eatmate.weblogout.service.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LogoutController {

    @Autowired
    private LogoutService logoutService;

    // 서버에서 provider 정보 제공(JSON 형식으로 전달하는 방식)
    @GetMapping("/get-provider")
    public ResponseEntity<Map<String, String>> getProvider(HttpSession session) {
        String provider = (String) session.getAttribute("provider");
        if (provider == null) {
            provider = "local"; // 일반 로그인인 경우
        }
        Map<String, String> response = new HashMap<>();
        response.put("provider", provider);
        return ResponseEntity.ok(response);
    }

    // 카카오 로그아웃
    @GetMapping("/kakao/logout")
    public void kakaologout(HttpServletResponse response, HttpSession session) throws IOException {
        String accessToken = (String) session.getAttribute("kakaoAccessToken");

        if (accessToken != null) {
            // 액세스 토큰 무효화
            logoutService.kakaoLogout(accessToken);
            session.removeAttribute("kakaoAccessToken");
            session.invalidate();   // 세션 무효화
        }
        // 카카오 로그아웃 URL로 리다이렉트
        String requestUrl = "https://kauth.kakao.com/oauth/logout"
                + "?client_id=e14cb05b33510d6d6fb59bc77f202156"
                + "&logout_redirect_uri=http://localhost:8080";     // 카카오 로그아웃에 저장한 url

        System.out.println("카카오 로그아웃 성공");
        response.sendRedirect(requestUrl);
        session.invalidate();
    }

    // 네이버 로그아웃
    @GetMapping("/naver/logout")
    public String naverLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("세션이 만료되었거나 존재하지 않습니다.");
            return "redirect:/login";
        }

        String accessToken = (String) session.getAttribute("naverAccessToken");
        System.out.println("네이버 로그아웃 액세스 토큰: " + accessToken); // 로그 추가
        if (accessToken == null) {
            System.out.println("액세스 토큰이 없습니다.");
            return "redirect:/login";
        }

        // 네이버 로그아웃 요청 (연동 해제)
        logoutService.naverLogout(accessToken);

        // 세션 무효화
        session.invalidate();

        System.out.println("네이버 로그아웃 성공");
        return "redirect:/";
    }

    // 구글 로그아웃
    @GetMapping("/google/logout")
    public String googleLogout(HttpSession session) {
        session.invalidate();  // 세션 무효화
        System.out.println("구글 로그아웃 성공");
        return "redirect:https://accounts.google.com/logout";
    }

}
