package com.eatmate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EatmateApplicationTests {

	/*
	 * RedisMessageListenerContainer는 SmartLifecycle이라 컨텍스트 기동 시 실제로
	 * 구독을 시도한다. Redis가 떠 있지 않으면 여기서 ConnectionFailure로 죽으므로
	 * 컨테이너만 목으로 대체한다. Pub/Sub 동작 자체는 이 테스트의 관심사가 아니다.
	 */
	@MockBean
	private RedisMessageListenerContainer redisMessageListenerContainer;

	@Test
	void contextLoads() {
	}

}
