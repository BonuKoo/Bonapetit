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
    public void kakaologout(String accessToken) {
        String url = "https://kapi.kakao.com/v1/user/logout";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // 로그아웃 요청 상태 코드 및 응답 로그 출력
        logger.info("Logout request URL: {}", url);
        logger.info("Logout Response Status Code: {}", response.getStatusCode());
        logger.info("Logout Response Body: {}", response.getBody());

        if(response.getStatusCode().is2xxSuccessful()){
            logger.info("Successfully logged out from Kakao");
        } else {
            logger.error("Failed to logout from Kakao, status code : {}", response.getStatusCode());
        }
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
        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

        // 로그아웃 요청 상태 코드 및 응답 확인
        logger.info("Naver Logout request URL: {}", url);
        logger.info("Naver Logout Response Status Code: {}", response.getStatusCode());
        logger.info("Naver Logout Response Body: {}", response.getBody());

        if(response.getStatusCode().is2xxSuccessful()){
            logger.info("Successfully logged out from Naver");
        } else {
            logger.error("Failed to logout from Naver, status code : {}", response.getStatusCode());
        }
    }

    // 구글 로그아웃
    public void googleLogout() {
        // 구글 로그아웃은 클라이언트 측에서 처리
        String googleLogoutUrl = "https://accounts.google.com/logout";
        logger.info("Google Logout request URL: {}", googleLogoutUrl);
        // 클라이언트 쪽에서 리디렉트로 처리
    }

}
