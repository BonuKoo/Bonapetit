package com.eatmate.weblogout.controller;

import com.eatmate.account.service.AccountService;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.weblogout.service.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Controller
public class LogoutController {

    @Autowired
    private LogoutService logoutService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    // 카카오 로그아웃
    @GetMapping("/kakao/logout")
    public void kakaologout(HttpServletResponse response, HttpSession session) throws IOException {
        String accessToken = (String) session.getAttribute("kakaoAccessToken");

        // 액세스 토큰 확인을 위해 로그 출력
        System.out.println("액세스 토큰: " + accessToken);

        if (accessToken != null){
            // 액세스 토큰 무효화
            logoutService.kakaologout(accessToken);
            session.removeAttribute("kakaoAccessToken");
            session.invalidate();   // 세션 무효화
        }
        // 카카오 로그아웃 URL로 리다이렉트
        String requestUrl = "https://kauth.kakao.com/oauth/logout"
                + "?client_id=e14cb05b33510d6d6fb59bc77f202156"
                + "&logout_redirect_uri=http://localhost:8080";     // 카카오 로그아웃에 저장한 url

        response.sendRedirect(requestUrl);
    }

    // 네이버 로그아웃
    @GetMapping("/naver/logout")
    public String naverLogout(HttpSession session) {
        String accessToken = (String) session.getAttribute("naverAccessToken");  // 세션 또는 DB에서 토큰 가져오기
        if (accessToken != null) {
            String apiUrl = "https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=vxzSX7hpBWdUiZGim9aX"
                    + "&client_secret=STysDb2fkw&access_token=" + accessToken
                    + "&service_provider=NAVER";

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, null, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                session.invalidate();  // 세션 무효화
            }
        }
        return "redirect:/";
    }

    // 구글 로그아웃
    @GetMapping("/google/logout")
    public String googleLogout(HttpSession session) {
        session.invalidate();  // 세션 무효화
        return "redirect:https://accounts.google.com/logout";
    }

}
