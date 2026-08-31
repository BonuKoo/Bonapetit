package com.eatmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

// JPA Auditing은 db.config.JpaAuditingConfig로 분리했다.
// 여기에 두면 슬라이스 테스트가 설정 루트로 이 클래스를 읽으면서 감사 기능까지
// 끌고 들어와 JPA 메타모델이 없다며 죽는다.
@SpringBootApplication
		//(exclude = {SecurityAutoConfiguration.class})
public class EatmateApplication {

	public static void main(String[] args) {
		SpringApplication.run(EatmateApplication.class, args);
	}

}
