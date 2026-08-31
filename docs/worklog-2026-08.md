# 작업 기록 — 2026-08 채팅 영속화 및 설정 분리

| 항목 | 내용 |
|---|---|
| 기간 | 2026-08-31 |
| 대상 | `master` |
| 결과 | PR [#69](https://github.com/BonuKoo/Bonapetit/pull/69) · [#70](https://github.com/BonuKoo/Bonapetit/pull/70) 머지 |

1년 가까이 방치된 병합 충돌로 부팅조차 되지 않던 상태에서 시작해, 설정 구조를 정리하고 채팅 메시지 영속화를 구현하기까지의 기록입니다.

---

## 1. 결과 요약

| 영역 | 시작 | 종료 |
|---|---|---|
| 애플리케이션 설정 | 충돌 마커가 남아 유효한 YAML이 아님 — **부팅 불가** | 공통 / dev / prod 3파일 분리, 부팅 확인 |
| 채팅 메시지 | **어디에도 저장되지 않음** — 내역 조회 불가능 | RDBMS 동기 영속화 + 커서 기반 조회 API |
| 테스트 | `contextLoads()`조차 실패, 외부 의존 3종 필요 | **28건 통과**, 외부 의존 없이 실행 |
| 스키마 수명 | `ddl-auto: create-drop` — 재시작마다 전체 삭제 | `update`로 복구 |
| 운영 시크릿 | RDS 계정·OAuth 키 평문 하드코딩 | 환경변수화, dev만 더미 fallback |
| 지도 SDK | 앱 정지 상태로 로드 실패 | 키 교체 + 카카오맵 활성화, 응답 확인 |

| 지표 | 값 |
|---|---|
| 테스트 | 28건 (시작 시 0) |
| 머지된 PR | 2건 · 커밋 11개 |
| 검증 메시지 | 234건 — 커서 경계 중복·누락 0 |
| 설정 파일 | 1개(419줄) → 3개(283줄) |

---

## 2. 진단

세 건 모두 처음 보고된 증상과 실제 원인이 달랐습니다.

### 2.1 좀비 병합 충돌

| 단계 | 내용 |
|---|---|
| **증상** | `application.yml`에 `<<<<<<<` 마커가 남아 유효한 YAML이 아님 |
| **진단** | `.git/MERGE_HEAD`는 없는데 인덱스에는 충돌 스테이지가 남아 git은 "병합 중 아님"으로 인식. `MERGE_MSG` 타임스탬프가 **2026-08-14** — 약 1년 방치 |
| **조치** | 환경변수화된 쪽 기준으로 해소. 작업 트리에는 이미 병합 결과가 반영돼 있었고 이 파일만 미해결이었음 |
| **검증** | `git ls-files -u` 결과 없음. 이후 부팅 성공 |

### 2.2 메시지가 저장된 적이 없음

| 단계 | 내용 |
|---|---|
| **증상** | 서버가 꺼지면 대화가 사라진다 |
| **진단** | `ChatMessage`가 순수 DTO였고 엔티티·리포지토리가 존재하지 않음. Redis Pub/Sub은 fire-and-forget이라 구독 중이 아니면 소멸. 더해서 `ddl-auto: create-drop`이 RDBMS도 함께 비우고 있었음 |
| **조치** | 엔티티·리포지토리 신설, 발행 지점에서 동기 INSERT |
| **검증** | 실사용 234건 저장, 재시작 후에도 유지 |

### 2.3 지도가 뜨지 않는 이유는 HTTPS가 아니었다

| 단계 | 내용 |
|---|---|
| **증상** | localhost에서 지도 호출 실패 → "HTTPS가 필요하니 배포해야 한다"는 판단 |
| **진단** | SDK를 http·https 양쪽으로 요청한 결과 **둘 다 500**, 응답은 `SuspendedAppException`. 앱 자체가 정지 상태여서 프로토콜과 무관했고 배포로도 해결되지 않는 문제였음 |
| **조치** | 살아있는 앱의 JS 키로 교체(4개 템플릿) + 카카오맵 제품 활성화 |
| **검증** | 교체 후 200 응답, 실제 SDK 코드 수신 확인 |

> **진단 방법** — 존재하지 않는 키로 같은 요청을 보내면 `appKey does not exist`가 돌아옵니다. 실제 키는 `SuspendedAppException`이었으므로 "키가 틀렸다"가 아니라 "키는 맞는데 앱이 정지됐다"가 확정됐습니다. 비교군을 만들어 원인을 좁힌 사례입니다.

---

## 3. PR #69 — 설정 프로필 분리

단일 `application.yml` 안에서 `---` 문서 겹침으로 환경을 나누던 구조를 프로필 파일로 분리했습니다. 기존 구조는 브랜치마다 같은 파일이 다르게 진화해 병합 때마다 충돌했고, 그것이 1년간 방치된 충돌의 원인이었습니다.

| 파일 | 키 | 줄 | 내용 |
|---|---|---|---|
| `application.yml` | 39 | 103 | OAuth provider 전체, registration 공통부, API 키, `ddl-auto`, `profiles.active: dev` |
| `application-dev.yml` | 21 | 64 | H2, H2Dialect, livereload, `{baseUrl}` redirect |
| `application-prod.yml` | 31 | 116 | MySQL, Redis 세션·풀, Hikari/Tomcat, Nginx 헤더, Prometheus |

### 동작 동등성 검증

원본 다중 문서를 병합한 결과와 새 구조를 병합한 결과를 프로퍼티 단위로 비교했습니다.

```
===== DEV : 차이 0건
===== PROD : 차이 4건
   spring.logging.level.org.hibernate
   spring.logging.level.org.springframework.cache
   spring.logging.level.org.springframework.cache.interceptor.CacheInterceptor
   spring.logging.level.org.springframework.security
```

PROD의 4건은 모두 `spring.logging.level.*`로, Spring이 바인딩하지 않는 죽은 키입니다(실제 키는 최상위 `logging.level.*`). 원본 구조에서 prod 문서가 기본 문서 위에 얹히며 개발용 DEBUG 설정이 운영으로 새던 것이라 의도적으로 채택하지 않았습니다. 반면 같은 경로로 새던 `format_sql`은 실제 동작하는 설정이라 공통으로 올려 기존 동작을 보존했습니다.

### 충돌 해소로 함께 정리된 것

| 항목 | 이전 | 이후 |
|---|---|---|
| `ddl-auto` | `create-drop` | `update` |
| 운영 DB 접속 | RDS 엔드포인트·계정 평문 | 환경변수 |
| OAuth 키 6개 | 평문 | 환경변수 필수 |
| Redis 키 | `spring.redis.*` (Boot 3에서 제거됨) | `spring.data.redis.*` |
| `prod` 프로필 | **2회 중복 정의** | 1회 |

> `spring.redis.*` 문제는 실제 부팅 실패 요인이었습니다. `RedisConfig`가 `${spring.data.redis.host}`를 읽는데 설정은 구 키만 제공하고 있어, 충돌을 다른 쪽으로 해소했다면 앱이 뜨지 않았습니다.

---

## 4. PR #70 — 커서 기반 채팅 내역 조회

### 저장 지점 — Pub/Sub 중복 저장 회피

```
ChatController.message()  ──> ChatService.sendChatMessage()
                                  ├─ (TALK이면) save()          ← 여기
                                  └─ convertAndSend(topic, dto)
                                          │
                                          ↓ (구독 중인 모든 인스턴스가 수신)
                                     RedisSubscriber.sendMessage()   ← 여기 두면 N번 저장
```

Pub/Sub은 구독 중인 모든 서버 인스턴스에 전달되므로, 수신 측에서 저장하면 서버 N대에서 같은 메시지가 N번 저장됩니다. 발행 지점은 메시지당 정확히 1회 실행됩니다.

### 설계 결정

| 항목 | 선택 | 근거 |
|---|---|---|
| 저장 방식 | 동기 INSERT | 비동기는 서버 다운 시 버퍼 유실, Redis 버퍼링은 조회가 두 소스에 걸쳐 커서 페이징이 쪼개짐. 병목으로 측정된 바 없음 |
| 커서 키 | `id` | `createdAt`은 같은 밀리초에 여러 건이 들어올 수 있어 부적합 |
| 인덱스 | `(room_id, chat_message_id)` | 커서 쿼리가 이 인덱스만으로 처리됨 |
| 페이징 반환형 | `List` + `Pageable` | `Page`는 매번 `count(*)`를 추가로 날림. 채팅 내역에 전체 건수는 불필요 |
| `hasMore` | `size+1` 조회 | count 쿼리 없이 다음 페이지 존재 여부 판정 |
| 저장 대상 | TALK만 | 입퇴장 알림은 재접속이 잦으면 실제 대화보다 많아져 내역에 노이즈 |
| `senderName` | 발신 시점 스냅샷 | 닉네임을 바꿔도 과거 메시지 표시명이 보존됨 |
| size 상한 | 100 | 없으면 `size=100000`으로 방 전체를 한 번에 조회 가능 |
| QueryDSL | 사용 안 함 | 동적 조건이 없음. 기존 `NoticeRepository4QueryDsl`은 동적 검색 때문에 도입한 것 |

### 테스트 구성

| 계층 | 방식 | 건수 | 중점 |
|---|---|---|---|
| 저장소 | `@DataJpaTest` | 6 | 커서 경계, 방 격리, `hasMore` 판정 |
| `ChatService` | Mockito | 6 | TALK만 저장, 저장 실패 시 발행 차단 |
| `ChatHistoryService` | Mockito | 11 | 커서·정렬 반전·클램프·인가 |
| 컨트롤러 | `@WebMvcTest` | 4 | 응답 JSON, 파라미터 바인딩, 403 |
| 컨텍스트 | `@SpringBootTest` | 1 | 기동 |

> 테스트 인프라 자체가 이 PR에서 처음 생겼습니다. 기존 `contextLoads()`는 H2 TCP 서버에 붙지 못해 실패하고 있었고, 실행에 H2·Redis·환경변수 6개가 모두 필요했습니다. `application-test.yml`을 **오버레이 방식**으로 붙여 해결했습니다 — `test/resources/application.yml`로 두면 main 설정을 통째로 가려 OAuth registration이 사라지고 `ClientRegistrationRepository` 빈 생성에 실패합니다.

---

## 5. 검증 — 실사용 234건

커서 페이징에서 가장 잘 깨지는 지점은 경계입니다. `<`를 `<=`로 쓰면 페이지마다 1건씩 중복되고, 반대로 계산이 어긋나면 조용히 누락됩니다. 실제 데이터로 저장소 쿼리와 동일한 조건을 재현해 확인했습니다.

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
| ENTER/QUIT 미저장 | 여러 차례 입퇴장했음에도 234건 전부 TALK, 비-TALK **0건** |
| 유실 없음 | ID 1–234 연속 |
| 복합 인덱스 | 실 DB에서 `IDX_CHAT_MESSAGE_ROOM_ID = (ROOM_ID, CHAT_MESSAGE_ID)` 생성 확인 |
| FK 연결 | `sender` → ACCOUNT, `chatRoom` → CHATROOM 양쪽 정상 |
| 한글 저장 | UTF-8 바이트 직접 디코딩으로 확인 (콘솔 출력 깨짐은 H2 Shell의 cp949 문제) |

---

## 6. 곁들여 정리한 것

| 항목 | 내용 | 계기 |
|---|---|---|
| 중복 Redis 호출 제거 | `ChatController`와 `ChatService`가 `getUserCount()`를 각각 호출해 같은 값을 덮어쓰고 있었음 | 메시지당 INSERT 1회 추가의 비용을 따지다 발견. 왕복 1회를 줄여 순증가가 상쇄됨 |
| DTO 이름 정리 | `chat.dto.ChatMessage` → `ChatMessageDTO`. 엔티티가 `ChatMessage` 이름을 가져감 | 프로젝트 컨벤션이 `ChatRoom`/`ChatRoomDTO`인데 이것만 벗어나 있었음 |
| `MessageType` 이동 | DTO → 도메인 엔티티. DTO가 엔티티를 참조하는 방향으로 정정 | 엔티티가 DTO에 의존하는 역전을 피하기 위해 |
| `@EnableJpaAuditing` 분리 | 애플리케이션 클래스 → `db.config.JpaAuditingConfig` | `@WebMvcTest`가 애플리케이션 클래스를 설정 루트로 읽으며 `JPA metamodel must not be empty`로 실패 |
| 실시간 DTO에 `id` 추가 | 저장 후 발행 DTO에 id를 실어 보냄 | 템플릿의 `:key="message.id"`가 전부 `undefined`였음. 내역 조회분과 실시간 수신분의 중복 판별에도 사용 |
| 로컬 설정 분리 | `config/`를 gitignore에 추가. Spring Boot가 클래스패스보다 높은 우선순위로 자동 로드 | 실제 키를 추적되는 파일에 두면 커밋에 딸려 들어감 |
| dev 프로필 fallback | OAuth 키에 더미 기본값. 운영은 공통 설정을 상속해 그대로 엄격 | IDE 실행 시 환경변수 6개를 챙기지 않으면 부팅 실패 |

### 커밋

```
fa59088  test: 외부 의존 없이 돌아가는 테스트 프로필 추가
a6feafa  feat: 채팅 메시지 영속화 엔티티·저장소 추가
5307e8f  feat: 메시지 발행 시 RDBMS 동기 영속화
947343d  feat: 커서 기반 채팅 내역 조회 API
91ce365  feat: 채팅방 진입 시 이전 대화 내역 로드
b114bce  fix(config): dev 프로필에 OAuth 키 더미 fallback 추가
c14cf72  chore: 로컬 전용 설정 디렉터리 config/ 를 gitignore에 추가
2e9224e  fix: 카카오 지도 JavaScript 키 교체
```

---

## 7. 남은 과제

이 작업에서 식별했으나 범위를 벗어나 기록만 남긴 항목은 [알려진 이슈](known-issues.md)에 정리했습니다. 우선순위는 다음과 같습니다.

1. **ISS-01** 인가 검증 추가 — 이용자 데이터에 직접 피해가 발생할 수 있는 유일한 항목
2. **ISS-03** 카카오·네이버 키 재발급 및 코드 분리
3. **ISS-02** 권한 문자열 체계 통일

**기존 대화는 복구할 수 없습니다.** 지금까지 오간 메시지는 저장된 적이 없어, 이 기능은 배포 시점 이후 메시지부터 적용됩니다.

---

관련 문서: [요구사항 명세](requirements.md) · [시스템 명세](architecture.md) · [API 명세](api.md) · [알려진 이슈](known-issues.md)
