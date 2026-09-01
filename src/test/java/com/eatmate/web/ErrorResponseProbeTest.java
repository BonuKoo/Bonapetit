package com.eatmate.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 처리되지 않은 예외가 사용자에게 무엇을 보여주는지 실제로 확인한다.
 *
 * <p>ISS-11은 "전역 예외 처리기가 없어 내부 오류가 그대로 노출되며, 오류 응답에 스택 정보가
 * 포함될 수 있다"고 적고 있었다. 재 본 결과 <b>스택은 노출되지 않는다.</b> Spring Boot 3의
 * 기본값이 {@code include-stacktrace=never} · {@code include-message=never} 이고 이
 * 프로젝트에는 그 설정을 바꾼 곳이 없다.
 *
 * <p>대신 다른 두 가지가 드러났고, 그 둘을 고쳤다.
 * <ul>
 *   <li>상태 코드가 틀렸다. 없는 모임 id가 500이었다 -&gt; {@code GlobalExceptionHandler}로 400</li>
 *   <li>비로그인 사용자가 오류를 만나면 로그인 페이지로 튕겼다 -&gt; {@code /error}를 permitAll로</li>
 * </ul>
 *
 * <p>대상은 {@code /post/detail/{teamId}} 다. permitAll이라 로그인 없이 부를 수 있고,
 * 없는 id를 주면 {@code IllegalArgumentException}이 그대로 올라온다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ErrorResponseProbeTest {

    /** RedisMessageListenerContainer는 SmartLifecycle이라 기동 시 실제로 구독을 시도한다. */
    @MockBean private RedisMessageListenerContainer redisMessageListenerContainer;

    @LocalServerPort private int port;
    @Autowired private ServerProperties serverProperties;

    /** 리다이렉트를 따라가면 최종 응답만 보게 되어 원래 상태 코드를 놓친다. */
    private RestTemplate noRedirectRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        RestTemplate rest = new RestTemplate(factory);
        rest.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return rest;
    }

    @Test
    @DisplayName("오류 응답 설정이 스택과 메시지를 감추도록 되어 있다")
    void errorPropertiesHideInternals() {
        ErrorProperties error = serverProperties.getError();
        System.out.println("### include-stacktrace = " + error.getIncludeStacktrace());
        System.out.println("### include-message    = " + error.getIncludeMessage());

        // 문서에 "스택 정보가 포함될 수 있다"고 적혀 있었으나 실제 설정은 반대다.
        assertThat(error.getIncludeStacktrace()).isEqualTo(ErrorProperties.IncludeAttribute.NEVER);
        assertThat(error.getIncludeMessage()).isEqualTo(ErrorProperties.IncludeAttribute.NEVER);
    }

    @Test
    @DisplayName("없는 모임 id는 500이 아니라 400이고, 로그인으로 튕기지 않는다")
    void unknownTeamIdIsBadRequest() {
        ResponseEntity<String> response = noRedirectRestTemplate().getForEntity(
                "http://localhost:" + port + "/post/detail/999999", String.class);

        String body = response.getBody() == null ? "" : response.getBody();
        System.out.println("### status = " + response.getStatusCode());
        System.out.println("### body   = " + body.replace(System.lineSeparator(), " ").trim());

        // 고치기 전에는 302 -> /login 이었다. /error 가 permitAll 이 아니어서
        // 오류 디스패치까지 보안 필터에 걸렸기 때문이다.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 상태 코드만 바로잡을 뿐 내부 정보는 여전히 새지 않아야 한다.
        assertThat(body)
                .doesNotContain("java.lang.IllegalArgumentException")
                .doesNotContain("at com.eatmate")
                .doesNotContain("Invalid team ID");
    }
}
