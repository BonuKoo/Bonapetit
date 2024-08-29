package com.eatmate.kakao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(KakaoService.class);

    public String getAccessToken(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        RestTemplate restTemplate = new RestTemplate();

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id","4d77a5adb4a0ec1e3c0b6b28c5f48284");     // REST API 키
        params.add("redirect_uri","http://localhost:8080/oauth/kakao/callback");    // 리다이렉트 URI
        params.add("code", code);
        params.add("client_secret", "cn8DCCeldGOtP4xsKpmLskR4pPTD7zwO"); // 필요시 추가

        // HttpEntity 생성
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        logger.info("Authorization code received: {}", code);

        try {
            // POST 요청 보내기
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                return (String) responseBody.get("access_token");
            } else {
                logger.error("Failed to get access token, status code: {}", response.getStatusCode());
                throw new RuntimeException("Failed to get access token");
            }
        } catch (HttpClientErrorException e) {
            logger.error("HttpClientErrorException: {}", e.getMessage());
            logger.error("Response body: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get access token due to client error", e);
        } catch (Exception e) {
            logger.error("Exception: {}", e.getMessage());
            throw new RuntimeException("Failed to get access token", e);
        }
    }

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

        if(response.getStatusCode().is2xxSuccessful()){
            logger.info("Successfully logged out from Kakao");
        }else {
            logger.error("Failed to logout from Kakao, status code : {}", response.getStatusCode());
        }
    }

    public void unlink(String accessToken) {
        String url = "https://kapi.kakao.com/v1/user/unlink";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        restTemplate.postForEntity(url, entity, String.class);
    }
}