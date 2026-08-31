package com.eatmate.db.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정.
 *
 * 원래 EatmateApplication(@SpringBootApplication)에 붙어 있었는데, 그러면
 * @WebMvcTest 같은 슬라이스 테스트가 애플리케이션 클래스를 설정 루트로 읽으면서
 * 감사 기능까지 끌고 들어와 "JPA metamodel must not be empty"로 죽는다.
 * 슬라이스 테스트는 JPA를 구성하지 않기 때문이다.
 *
 * 별도 @Configuration으로 분리하면 슬라이스 테스트의 타입 필터가 걸러주고,
 * 런타임 동작은 그대로다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
