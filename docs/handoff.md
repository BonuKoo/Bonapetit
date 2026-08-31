# 이어가기 프롬프트

> **사용법** — 이 파일 전체를 새 AI 세션이나 새 작업자에게 그대로 전달하면 맥락 없이도 바로 작업을 이어갈 수 있습니다.
> 2026-08-31 기준이며, 작업이 진행되면 이 문서도 함께 갱신해 주세요.

---

## 1. 프로젝트

**EatMate (리포명 Bonapetit)** — 같은 지역 사람들이 함께 식사할 모임을 만들고, 참여하고, 전용 채팅방에서 실시간으로 대화하는 커뮤니티 서비스.

| 항목 | 값 |
|---|---|
| 로컬 경로 | `F:\TeamProject\eatmate` |
| GitHub | https://github.com/BonuKoo/Bonapetit |
| 스택 | Java 17 · Spring Boot 3.3.3 · JPA + MyBatis + QueryDSL · Redis · MySQL/H2 |
| 뷰 | Thymeleaf · Vue 2 · STOMP/SockJS · 카카오 지도 SDK |
| 작업 브랜치 | `master` |
| 테스트 | 50건 (채팅 도메인 한정) |

**⚠️ 리포 이름 주의** — `git remote`는 `BonuKoo/eatMate.git`인데 GitHub에서 `Bonapetit`으로 이름이 바뀌었습니다. **같은 리포**이며 구 URL은 리다이렉트됩니다. 별개 리포로 착각하지 마세요.

**⚠️ 기본 브랜치 주의** — GitHub 기본 브랜치가 `main`인데, 이건 `master`와 **공통 조상이 없는 고아 히스토리**입니다(README 커밋 4개뿐). **PR을 만들 때 base는 반드시 `master`로 명시**해야 합니다. 기본값(`main`)으로 두면 PR이 성립하지 않습니다.

---

## 2. 먼저 읽을 문서

| 문서 | 내용 |
|---|---|
| [README](../README.md) | 프로젝트 개요, 기술적 판단과 근거, 아키텍처 |
| [요구사항 명세](requirements.md) | 사용자 요구사항 14건, 인수 기준, 추적성 매트릭스 |
| [시스템 명세](architecture.md) | 구성요소, 데이터 모델, Redis 사용, 쿼리 분석, 캐시 설계 |
| [API 명세](api.md) | HTTP 41건 + STOMP 3건 |
| [의사결정 기록](decisions.md) | 구조에 영향을 준 결정 7건과 근거. **왜 그렇게 만들었는지는 여기** |
| [테스트 전략](testing.md) | 테스트 구성 · 인프라 · 실환경 검증 |
| [알려진 이슈](known-issues.md) | **결함 12건과 조치 우선순위 — 다음 작업은 여기서 고르면 됩니다** |
| [작업 기록](worklog-2026-08.md) | 2026-08 작업 전체 이력과 검증 데이터 |

---

## 3. 로컬 실행

```bash
# 1) Redis
docker run -d --name eatmate-redis -p 6379:6379 redis

# 2) H2 TCP 서버 — -ifNotExists 없으면 H2 2.x가 원격 DB 생성을 거부한다
java -cp <h2.jar> org.h2.tools.Server -tcp -ifNotExists -web

# 3) 실행 — 환경변수 불필요 (dev 프로필에 더미 fallback이 있다)
./gradlew bootRun
```

실제 OAuth 키는 **`config/application.yml`** 에 둡니다. gitignore 대상이고, Spring Boot가 클래스패스 설정보다 높은 우선순위로 자동 로드하므로 IDE에서 그냥 Run 해도 적용됩니다.

운영 프로필은 `SPRING_PROFILES_ACTIVE=prod` 와 함께 `DB_URL` · `DB_USERNAME` · `DB_PASSWORD` · `REDIS_HOST` · `JWT_SECRET` · OAuth 키 6종을 환경변수로 요구합니다.

---

## 4. 반드시 알아야 할 함정

이걸 모르면 시간을 크게 낭비합니다. 모두 실제로 겪은 것들입니다.

| # | 함정 | 대응 |
|---|---|---|
| 1 | `bootRun`을 중단해도 **Gradle 데몬의 자식 java가 8080을 계속 점유**한다 | `Get-NetTCPConnection -LocalPort 8080` 으로 PID를 찾아 `Stop-Process` |
| 2 | H2 Shell 출력이 **cp949라 한글이 깨져 보인다**. DB는 멀쩡하다 | `SELECT RAWTOHEX(STRINGTOUTF8(col))` 로 바이트를 직접 확인 |
| 3 | H2 TCP 서버에 `-ifNotExists` 를 빼면 **원격 DB 생성을 거부**한다 | 항상 붙일 것 |
| 4 | **테스트 설정을 `test/resources/application.yml` 로 두면 안 된다.** main 설정을 통째로 가려 OAuth registration이 사라지고 `ClientRegistrationRepository` 빈 생성에 실패한다 | `application-test.yml` + `@ActiveProfiles("test")` 오버레이 방식을 유지 |
| 5 | `@WebMvcTest` 가 `JPA metamodel must not be empty` 로 죽는다 | `@EnableJpaAuditing` 을 애플리케이션 클래스가 아닌 `db.config.JpaAuditingConfig` 에 두었기 때문에 지금은 해결됨. 다시 옮기지 말 것 |
| 6 | `@SpringBootTest` 가 Redis 없이 죽는다 | `RedisMessageListenerContainer` 는 SmartLifecycle이라 기동 시 구독을 시도한다. `@MockBean` 으로 대체 |
| 7 | **H2와 MySQL의 실행 계획이 다르다** | 성능 판단은 반드시 MySQL에서. H2만 보고 결론 내면 틀린다 (→ [작업 기록 6.3](worklog-2026-08.md)) |
| 8 | Gradle 홈이 기본 위치가 아니다 | `GRADLE_USER_HOME=F:\gradle` |
| 9 | 운영 설정 중 **바인딩되지 않는 키 3종**이 있다 | `spring.server.port`, `spring.logging.level.*`, `spring.session.servlet.session.timeout` → [ISS-08](known-issues.md#iss-08) |

---

## 5. 지금까지 한 일 (2026-08-31)

시작 시점에는 **1년 가까이 방치된 병합 충돌로 앱이 부팅조차 되지 않는 상태**였습니다.

| PR | 내용 |
|---|---|
| [#69](https://github.com/BonuKoo/Bonapetit/pull/69) | 좀비 병합 충돌 해소 + `application.yml` 을 공통/dev/prod 3파일로 분리 |
| [#70](https://github.com/BonuKoo/Bonapetit/pull/70) | 채팅 메시지 RDBMS 영속화 + 커서 기반 내역 조회 API + 프론트 무한 스크롤 |
| [#71](https://github.com/BonuKoo/Bonapetit/pull/71) | README 정정·보강, `docs/` 명세서 체계 신설 |
| [#72](https://github.com/BonuKoo/Bonapetit/pull/72) | 방 진입 캐시(최신 메시지 + 멤버십) + 쿼리 분석 + MySQL 재측정 |

| 영역 | 시작 | 현재 |
|---|---|---|
| 애플리케이션 | 부팅 불가 | 정상 기동 |
| 채팅 메시지 | 어디에도 저장 안 됨 | RDBMS 영속화 + 커서 조회 + Redis 캐시 |
| 테스트 | `contextLoads()` 조차 실패 | **50건 통과**, 외부 의존 없이 실행 |
| 문서 | 없음(커밋 안 된 README 하나) | 명세 4종 + 작업 기록 |

---

## 6. 다음에 할 일 — 우선순위

### 1순위 · [ISS-01](known-issues.md#iss-01) 인가 검증 추가 🔴

**이용자 데이터에 직접 피해가 발생할 수 있는 유일한 항목입니다.**

- `PostController.updateTeam` · `deleteTeam` · `kickMember` — 요청자가 개설자인지 확인하지 않음
- `AccountProfileController.leaveTeam` — `account_id` 를 파라미터로 받고 세션 주체와 대조하지 않음
- `StompHandler` SUBSCRIBE — 구독 시 방 참여자 여부를 확인하지 않음. `roomId` 만 알면 남의 대화를 실시간 열람 가능

**착수 비용이 낮습니다.** `ChatHistoryService.verifyMembership` 에 이미 멤버십 검증 패턴이 구현돼 있어 그대로 재사용할 수 있습니다. 멤버십 캐시(`ChatCacheRepository`)도 활용 가능합니다.

### 2순위 · [ISS-03](known-issues.md#iss-03) 키 재발급 및 코드 분리 🟠

카카오 REST 키가 `LogoutController` 에, 네이버 `client_id`/`client_secret` 이 `LogoutService` 에 하드코딩되어 공개 저장소에 노출 중입니다. **코드 수정과 콘솔 재발급을 함께** 해야 의미가 있습니다.

### 3순위 · [ISS-02](known-issues.md#iss-02) 권한 체계 통일 🟠

권한 문자열이 `ROLE_USER`/`ROLE_ADMIN`(소셜), `USER_ROLE`(폼 가입), `USER_ROLE`/`USER_ADMIN`(enum)으로 갈려 있습니다. 또 `AccountProfileController.updateEmployee` 가 인증 객체를 재구성하며 권한을 `ROLE_USER` 로 하드코딩해, **관리자가 표시 이름을 바꾸면 그 세션에서 관리자 권한을 잃습니다.**

### 그 밖

- [ISS-08](known-issues.md#iss-08) 바인딩 안 되는 설정 키 3종 교정 (수정 비용 낮음. 단 로그 레벨 적용 시 dev가 `root: DEBUG` 라 출력량 급증 주의)
- [ISS-12](known-issues.md#iss-12) 모임 목록 N+1 (10건 페이지당 최대 21회 쿼리)
- [ISS-05](known-issues.md#iss-05) 프로필의 빈 화면 2개에 데이터 연결
- **브랜치 정리** — 기본 브랜치를 `main` → `master` 로 변경, 머지된 브랜치 삭제, 방치된 개인 브랜치 정리
- **부하 테스트** — `application-prod.yml` 의 Tomcat 스레드 50 · Hikari 20 은 **잠정값**이며 "k6 재측정 후 조정" 주석이 달려 있습니다. STOMP를 기본 지원하는 부하 도구가 없으므로 `WebSocketStompClient` 기반 JVM 부하 생성기가 현실적입니다
- **계정·모임·공지 도메인 테스트** — 자동 테스트 50건이 전부 채팅 도메인에 있어 나머지는 회귀 안전망이 없습니다

---

## 7. 이 프로젝트에서 지켜온 작업 방식

문서와 코드가 이 원칙으로 쓰였습니다. 이어서 작업할 때도 유지하면 일관성이 유지됩니다.

- **추정하지 말고 측정한다.** 쿼리 수는 Hibernate `Statistics` 로, 실행 계획은 `EXPLAIN ANALYZE` 로 확인했습니다. 코드를 읽어 센 것과 실제가 달랐던 사례가 여러 번 있습니다.
- **개발 DB만 보고 결론 내지 않는다.** H2와 MySQL의 실행 계획이 갈려 한 번 결론이 뒤집혔습니다.
- **틀린 것은 문서에 남긴다.** 정정 이력을 지우지 않고 "무엇을 왜 뒤집었는지"를 기록했습니다.
- **미충족·미구현을 숨기지 않는다.** 명세서에 구현 상태를 함께 표기하고, 알려진 이슈를 별도 문서로 관리합니다.
- **커밋 메시지에 근거를 적는다.** "무엇을"보다 "왜 그 선택을 했는지"를 남깁니다.
- **캐시는 가용성, 영속화는 정합성.** 캐시는 모든 Redis 오류를 삼키고 폴백하며, 영속화는 저장 실패 시 발행까지 막습니다. 이 비대칭이 의도된 설계입니다.

---

## 8. 새 세션에 전달할 요약

위 내용을 다 읽기 어렵다면 아래 문단만 전달해도 됩니다.

> `F:\TeamProject\eatmate` 의 Spring Boot 프로젝트(GitHub: BonuKoo/Bonapetit)를 이어서 작업해 주세요.
> 먼저 `docs/handoff.md` 와 `docs/known-issues.md` 를 읽고 시작하세요.
> PR을 만들 때 base는 반드시 `master` 로 지정해야 합니다(기본 브랜치 `main` 은 코드와 무관한 고아 브랜치입니다).
> 다음 작업은 ISS-01(인가 검증 누락)이며, `ChatHistoryService.verifyMembership` 의 패턴을 재사용하면 됩니다.
> 로컬 실행은 Redis + H2 TCP 서버(`-ifNotExists` 필수) 를 띄운 뒤 `./gradlew bootRun` 이며 환경변수는 필요 없습니다.
> 성능 판단은 H2가 아니라 MySQL에서 해야 합니다. 두 DB의 실행 계획이 다릅니다.

---

관련 문서: [README](../README.md) · [요구사항](requirements.md) · [시스템](architecture.md) · [API](api.md) · [의사결정](decisions.md) · [테스트](testing.md) · [알려진 이슈](known-issues.md) · [작업 기록](worklog-2026-08.md)
