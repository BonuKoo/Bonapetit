# 테스트 전략

| 항목 | 내용 |
|---|---|
| 문서 버전 | 1.0 |
| 작성 기준일 | 2026-08-31 |
| 기준 커밋 | `master` · `cd0c976` |

---

## 실행

```bash
./gradlew test
```

**외부 의존 없이 실행됩니다.** H2 서버 · Redis · 환경변수 없이 그대로 돌아갑니다.

리포트는 `build/reports/tests/test/index.html` 에 생성됩니다.

---

## 테스트 인프라

테스트 인프라 자체가 2026-08 작업에서 처음 생겼습니다. 그전에는 `EatmateApplicationTests.contextLoads()` 조차 실패하고 있었고, 실행에 H2 TCP 서버 · Redis · OAuth 환경변수 6개가 모두 필요했습니다.

### 설정은 오버레이 방식이어야 합니다

`src/test/resources/application-test.yml` + `@ActiveProfiles("test")` 조합을 씁니다.

```
main/application.yml        공통 설정 (OAuth registration/provider 구조)
   └─ test/application-test.yml   그 위에 얹혀 datasource·비밀값만 덮어씀
```

- 인메모리 H2로 datasource를 교체합니다
- `@ActiveProfiles("test")` 가 main의 `profiles.active: dev` 를 덮으므로 `application-dev.yml`(H2 TCP 접속)은 로드되지 않습니다
- 환경변수를 요구하는 `kakao.client.id` 등에 더미값을 넣어 `${KAKAO_CLIENT_ID}` 플레이스홀더가 아예 평가되지 않게 합니다

> ⚠️ **`test/resources/application.yml` 로 두면 안 됩니다.** 그러면 main 설정을 통째로 가려 OAuth registration이 사라지고 `ClientRegistrationRepository` 빈 생성에 실패합니다. 처음에 이 방식으로 시도했다가 실패했습니다.

### `@SpringBootTest` 는 Redis 컨테이너를 목으로 대체합니다

`RedisMessageListenerContainer` 는 `SmartLifecycle` 이라 컨텍스트 기동 시 실제로 구독을 시도합니다. Redis가 없으면 여기서 `RedisConnectionFailureException` 으로 죽습니다. `@MockBean` 으로 대체합니다.

### `@EnableJpaAuditing` 위치

애플리케이션 클래스가 아니라 `db.config.JpaAuditingConfig` 에 있습니다. 애플리케이션 클래스에 두면 `@WebMvcTest` 가 그것을 설정 루트로 읽으면서 감사 기능까지 끌고 들어와 `JPA metamodel must not be empty` 로 실패합니다. **다시 옮기지 마세요.**

---

## 계층별 구성 — 50건

| 계층 | 방식 | 건수 | 중점 |
|---|---|---|---|
| 저장소 | `@DataJpaTest` | 6 | 커서 경계(중복·누락), 방 격리, `hasMore` 판정 |
| `ChatCacheRepository` | Mockito | 10 | **Redis 장애 격리**, LPUSHX 조건부 갱신, 통째 교체 |
| `ChatService` | Mockito | 8 | TALK만 저장, 저장 실패 시 발행 차단, 캐시 반영 |
| `ChatHistoryService` | Mockito | 17 | 커서 · 정렬 반전 · size 클램프 · 인가 · 캐시 히트/미스 |
| 쿼리 수 측정 | `@DataJpaTest` + Hibernate `Statistics` | 4 | 방 진입 쿼리 4 → 1 → 0 회귀 방지 |
| 컨트롤러 | `@WebMvcTest` | 4 | 응답 JSON, 파라미터 바인딩, 403 |
| 컨텍스트 | `@SpringBootTest` | 1 | 기동 |

### 무엇에 무게를 뒀나

**커서 경계** — 커서 페이징에서 가장 잘 깨지는 지점입니다. `<` 를 `<=` 로 쓰면 페이지마다 1건씩 중복되고, 반대로 계산이 어긋나면 조용히 누락됩니다. 저장소 테스트 6건 중 3건이 경계 검증입니다.

**Redis 장애 격리** — `ChatCacheRepository` 10건 중 **6건이 장애 격리**입니다. 캐시의 핵심 계약이 "Redis가 죽어도 서비스는 살아있다"이기 때문입니다([ADR-006](decisions.md#adr-006-캐시는-가용성-영속화는-정합성을-우선한다)). 특히 멤버십 조회 실패가 `false` 를 반환하는지 검증합니다 — `true` 면 Redis 장애가 인가 우회로 이어집니다.

**쿼리 수** — 성능 수치가 아니라 **쿼리 개수의 회귀를 막는 장치**입니다. 연관관계 페치 전략이나 인가 로직이 바뀌어 쿼리가 늘면 여기서 걸립니다. 코드를 읽어 세는 것과 실제 실행은 다를 수 있어 Hibernate `Statistics` 로 측정합니다.

---

## 실환경 검증

자동 테스트로 덮이지 않는 부분은 실제 사용으로 확인했습니다.

### 커서 경계 — 실사용 234건

| 페이지 | ID 범위 | 표시 | `hasMore` | `nextCursor` |
|---|---|---|---|---|
| page1 | 234 … 185 | 50 | true | 185 |
| page2 | 184 … 135 | 50 | true | 135 |
| page3 | 134 … 85 | 50 | true | 85 |
| page4 | 84 … 35 | 50 | true | 35 |
| page5 | 34 … 1 | 34 | false | — |

| 검증 항목 | 결과 |
|---|---|
| 중복 없음 | page1이 185에서 끝나고 page2가 **184**에서 시작 — 경계가 배타적 |
| 누락 없음 | 185 → 184 연속, 건너뛴 ID 없음 |
| 합계 일치 | 50+50+50+50+34 = **234** = 전체 건수 |
| ENTER/QUIT 미저장 | 여러 차례 입퇴장했음에도 234건 전부 TALK |
| 유실 없음 | ID 1–234 연속 |
| 한글 저장 | UTF-8 바이트 직접 디코딩으로 확인 |

### 실행 계획 — MySQL

성능 판단은 **운영 DB(MySQL)에서** 합니다. H2와 실행 계획이 다릅니다.

| 쿼리 | H2 (방 234건) | MySQL (방 5,000건 / 총 11만 건) |
|---|---|---|
| 첫 페이지 | 방 전체 읽음 — `scanCount: 235` | **실제 51행** — `Backward index scan` |
| 커서 구간 | 인덱스 범위 — `scanCount: 101` | **실제 51행** |

자세한 내용은 [시스템 명세의 쿼리 분석](architecture.md)과 [ADR-005](decisions.md#adr-005-방-진입-경로를-캐싱한다-첫-페이지--멤버십)를 참조하세요.

---

## 한계

**자동 테스트가 채팅 도메인에 집중되어 있습니다.** 50건 전부 채팅 관련이며, 계정 · 모임 · 공지 도메인은 자동 검증이 없습니다. 해당 영역을 수정할 때 회귀를 잡아줄 안전망이 없다는 뜻입니다. → [알려진 이슈 NFR-06](known-issues.md)

**브라우저 동작은 수동 확인입니다.** 프론트의 무한 스크롤과 스크롤 위치 보정은 자동 테스트가 없고 실사용으로만 확인했습니다.

**부하 테스트가 없습니다.** `application-prod.yml` 의 Tomcat 스레드 50 · Hikari 20 은 **잠정값**입니다. STOMP를 기본 지원하는 부하 도구가 없어(k6 · Gatling · JMeter 모두 프레임 수작업 필요) `WebSocketStompClient` 기반 JVM 부하 생성기가 현실적입니다.

---

관련 문서: [시스템 명세](architecture.md) · [의사결정 기록](decisions.md) · [알려진 이슈](known-issues.md)
