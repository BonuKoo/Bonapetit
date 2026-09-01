package com.eatmate.loadtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

/**
 * 부하 하니스가 세션을 제대로 얻는지 단계별로 확인하는 진단용 테스트.
 *
 * 핸드셰이크가 101 대신 200을 받았을 때 원인을 좁히려고 만들었다.
 * 어느 단계에서 끊기는지 상태 코드로 보여준다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(LoadTestSecurityConfig.class)
@EnabledIfSystemProperty(named = "loadtest", matches = "true")
class HandshakeProbeTest {

    @LocalServerPort private int port;

    @Test
    @DisplayName("테스트 로그인 세션이 보호된 경로를 통과하는지 확인한다")
    void probe() {
        RestTemplate rest = new RestTemplate();
        rest.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        });

        String base = "http://localhost:" + port;

        ResponseEntity<String> login = rest.getForEntity(base + "/test-only/login?oauth2Id=probe-1", String.class);
        System.out.println("### 1) /test-only/login  status=" + login.getStatusCode());
        System.out.println("### 1) body(sessionId)   = " + login.getBody());
        System.out.println("### 1) Set-Cookie        = " + login.getHeaders().get(HttpHeaders.SET_COOKIE));

        String cookie = login.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .map(h -> h.split(";", 2)[0])
                .reduce((a, b) -> a + "; " + b).orElse("");
        System.out.println("### 1) 되돌려 보낼 쿠키 = " + cookie);
        HttpHeaders withCookie = new HttpHeaders();
        withCookie.add(HttpHeaders.COOKIE, cookie);
        HttpEntity<Void> entity = new HttpEntity<>(withCookie);

        ResponseEntity<String> rooms = rest.exchange(
                base + "/chat/rooms", HttpMethod.GET, entity, String.class);
        boolean loginPage = rooms.getBody() != null && rooms.getBody().contains("<!DOCTYPE html>");
        System.out.println("### 2) /chat/rooms (보호됨) status=" + rooms.getStatusCode()
                + "  로그인페이지로 튕겼나=" + loginPage + "  (false 여야 성공)");

        ResponseEntity<String> info = rest.exchange(
                base + "/ws-stomp/info", HttpMethod.GET, entity, String.class);
        System.out.println("### 3) /ws-stomp/info      status=" + info.getStatusCode());
        System.out.println("### 3) body                = " + info.getBody());

        ResponseEntity<String> raw = rest.exchange(
                base + "/ws-stomp/websocket", HttpMethod.GET, entity, String.class);
        System.out.println("### 4) /ws-stomp/websocket status=" + raw.getStatusCode()
                + "  (업그레이드 헤더 없이 호출한 것이라 200이 정상)");
        System.out.println("### 4) body                = "
                + (raw.getBody() == null ? "null" : raw.getBody().substring(0, Math.min(120, raw.getBody().length()))));
    }
}
