package com.eatmate.loadtest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 부하 하니스 전용 인증 우회.
 *
 * <h3>왜 필요한가</h3>
 * 메시지 한 건을 보내려면 네 관문을 지나야 한다.
 * <pre>
 *   /ws-stomp 핸드셰이크   인증된 HTTP 세션 (anyRequest().authenticated())
 *   STOMP CONNECT          JWT
 *   SUBSCRIBE              핸드셰이크 세션의 simpUser + 멤버십
 *   SEND                   JWT
 * </pre>
 * JWT는 시크릿을 알면 만들 수 있지만 <b>세션은 로그인해야 얻는다.</b> 소셜 로그인은
 * 스크립트로 통과할 수 없고, 폼 로그인은 주체를 식별하지 못한다(ISS-09 ·
 * {@code FormLoginPrincipalTest}). 그래서 테스트 스코프에만 존재하는 로그인 경로를 둔다.
 *
 * <h3>운영에 새지 않는다</h3>
 * 이 클래스는 {@code src/test} 에만 있고 {@link TestConfiguration} 이라 컴포넌트 스캔에
 * 걸리지 않는다. 쓰려는 테스트가 {@code @Import} 로 명시해야 활성화된다.
 *
 * <h3>운영 체인은 건드리지 않는다</h3>
 * {@code /test-only/**} 에만 걸리는 체인을 앞에 하나 더 둔다. 그 밖의 모든 경로는
 * 운영 {@code SecurityConfig} 가 그대로 처리하므로, <b>핸드셰이크 인가는 실제 필터
 * 체인을 통과한다.</b> 그것까지 우회하면 측정이 실제 경로와 달라진다.
 */
@TestConfiguration
public class LoadTestSecurityConfig {

    /** 운영 체인보다 먼저 평가된다. securityMatcher 밖의 요청에는 관여하지 않는다. */
    @Bean
    @Order(-1)
    public SecurityFilterChain testOnlyChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/test-only/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * 중첩 @RestController 는 설정 클래스의 멤버로서 자동 등록된다.
     * @Bean 으로 또 등록하면 핸들러 매핑이 중복돼 컨텍스트가 뜨지 않는다.
     */
    @RestController
    public static class TestLoginController {

        private final SecurityContextRepository repository = new HttpSessionSecurityContextRepository();

        /**
         * oauth2Id 를 주체로 하는 세션을 만들고 세션 id 를 돌려준다.
         *
         * principal 로 문자열을 넣으면 {@code Authentication.getName()} 이 그 문자열이 된다.
         * 이 앱이 주체를 식별하는 방식(= oauth2Id)과 같아진다.
         */
        @GetMapping("/test-only/login")
        public String login(@RequestParam String oauth2Id,
                            HttpServletRequest request,
                            HttpServletResponse response) {
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    oauth2Id, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            request.getSession(true);
            repository.saveContext(context, request, response);

            return request.getSession().getId();
        }
    }
}
