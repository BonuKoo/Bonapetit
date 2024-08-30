package com.eatmate.kakao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class KakaoService {

//    @Value("${kakao.client.id}")
//    private String kakaoClientId;
//
//    @Value("${kakao.client.secret}")
//    private String kakaoClientSecret;

    private static final Logger logger = LoggerFactory.getLogger(KakaoService.class);

    // 인증 코드로 액세스 토근 요청
    public String getAccessToken(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        RestTemplate restTemplate = new RestTemplate();

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", "e14cb05b33510d6d6fb59bc77f202156");     // REST API 키
        params.add("redirect_uri", "http://localhost:8080/oauth/kakao/callback");    // 리다이렉트 URI
        params.add("code", code);
        params.add("client_secret", "jBgKKQxp5icYdE8NdbWWP7wbJjTFMhcJ"); // 클라이언트 시크릿

        // 디버깅 로그 추가
        logger.info("Sending request to Kakao for access token with code: {}", code);
        logger.info("Request URL: {}", url);
        logger.info("Request parameters: {}", params);

        // HttpEntity 생성
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            // POST 요청 보내기
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            // 응답 로그 추가
            logger.info("Received response from Kakao: {}", response);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();

                // 응답 본문 로그 추가
                logger.info("Response body: {}", responseBody);

                // 액세스 토큰이 null인지 확인
                if (responseBody != null && responseBody.containsKey("access_token")) {
                    return (String) responseBody.get("access_token");
                } else {
                    logger.error("응답에 액세스 토큰이 포함되어 있지 않습니다.");
                    throw new RuntimeException("Failed to retrieve access token: response does not contain access_token");
                }
            } else {
                throw new RuntimeException("Failed to get access token: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get access token", e);
        }
    }


    // 액세스 토큰으로 사용자 정보 요청
    public Map<String, Object> getUserProfile(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to get user profile");
        }
    }

    // 카카오 로그아웃
    public void logout(String accessToken) {
        String url = "https://kapi.kakao.com/v1/user/logout";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // 로그아웃 요청 상태 코드 및 응답 로그 출력
        logger.info("로그아웃 요청 상태 코드: {}", response.getStatusCode());
        logger.info("로그아웃 응답 본문: {}", response.getBody());

        if(response.getStatusCode().is2xxSuccessful()){
            logger.info("Successfully logged out from Kakao");
        } else {
            logger.error("Failed to logout from Kakao, status code : {}", response.getStatusCode());
        }
    }

    // 카카오 연결
    public void unlink(String accessToken) {
        String url = "https://kapi.kakao.com/v1/user/unlink";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        restTemplate.postForEntity(url, entity, String.class);
    }
}