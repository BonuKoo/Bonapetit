package com.eatmate.weblogout.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LogoutService {

    private static final Logger logger = LoggerFactory.getLogger(LogoutService.class);

    // 카카오 로그아웃
    public void kakaoLogout(String accessToken) {
        String url = "https://kapi.kakao.com/v1/user/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        logger.info("카카오 로그아웃 성공");
    }

    // 네이버 로그인 연동 해제 (토큰 삭제)
    public void naverLogout(String accessToken) {
        // 네이버 연동 해제 API URL
        String url = "https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=vxzSX7hpBWdUiZGim9aX"
                + "&client_secret=STysDb2fkw"
                + "&access_token=" + accessToken
                + "&service_provider=NAVER";

        RestTemplate restTemplate = new RestTemplate();

        // POST 방식으로 요청
        restTemplate.postForEntity(url, null, String.class);

        logger.info("네이버 로그아웃 성공");
    }

    // 구글 로그아웃
//    public void googleLogout() {
//        // 구글 로그아웃은 클라이언트 측에서 처리
//        String googleLogoutUrl = "https://accounts.google.com/logout";
//        logger.info("구글 로그아웃 처리 URL: {}", googleLogoutUrl);
//    }

}
