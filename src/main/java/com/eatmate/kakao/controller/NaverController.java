package com.eatmate.kakao.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

public class NaverController {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/naver/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        // 로그 출력 시작
        System.out.println("네이버 로그아웃 시작");

        // 네이버에서 반환한 OAuth2 정보를 가져옵니다.
        OAuth2AuthenticationToken authentication = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );

        if (client != null) {
            // 네이버 액세스 토큰을 이용하여 연결을 해제합니다.
            String accessToken = client.getAccessToken().getTokenValue();
            String apiUrl = "https://nid.naver.com/oauth2.0/token" +
                    "?grant_type=delete&client_id=vxzSX7hpBWdUiZGim9aX" +
                    "&client_secret=STysDb2fkw&access_token=ACCESS_TOKEN";

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, null, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                System.out.println("네이버 로그아웃 성공: " + responseEntity.getBody());

                // 세션 무효화 및 Spring Security 컨텍스트 초기화
                session.invalidate();
                SecurityContextHolder.clearContext();
            } else {
                System.err.println("네이버 로그아웃 실패: " + responseEntity.getBody());
            }
        } else {
            System.err.println("네이버 인증 정보를 찾을 수 없습니다.");
        }

        // 로그아웃 후 홈 페이지로 리디렉션
        return "redirect:/";
    }



}
