# 시스템 명세서

| 항목 | 내용 |
|---|---|
| 문서 버전 | 2.0 |
| 작성 기준일 | 2026-08-31 |
| 기준 커밋 | `master` · `7ebe9b0` |

시스템의 구조, 구성 요소, 데이터 모델, 외부 연동, 비기능 특성을 기술합니다.

---

## 1. 시스템 개요

| 항목 | 내용 |
|---|---|
| 구조 | 단일 모놀리식 웹 애플리케이션. 서버 렌더링(Thymeleaf) 기반이며 일부 화면만 클라이언트 스크립트로 동적 처리 |
| 런타임 | Java 17 · Spring Boot 3.3.3 · 내장 Tomcat 10.1 |
| 영속성 | JPA(Hibernate 6.5) · MyBatis 3 · QueryDSL 5 **혼용** |
| 데이터 저장소 | MySQL 8 (운영) / H2 (개발) · Redis |
| 실시간 통신 | WebSocket · STOMP · SockJS |
| 인증 | OAuth 2.0(카카오·네이버·구글) · 세션 기반 · WebSocket 구간은 JWT |
| 클라이언트 | Thymeleaf · Vue 2 · Bootstrap 4 · 카카오 지도 JS SDK |

## 2. 구성 요소

| 계층 | 구성 요소 | 책임 |
|---|---|---|
| 표현 | `*Controller` (10종) | HTTP 요청 처리, 뷰 렌더링 또는 JSON 반환 |
| 표현 | `ChatController` · `StompHandler` | STOMP 메시지 수신, 연결·구독 시점 처리 |
| 서비스 | `ChatService` / `ChatHistoryService` | 메시지 발행·영속화 / 내역 조회·인가. **쓰기와 읽기 분리** |
| 서비스 | `PostJpaService` · `TeamJpaService` | 모임과 채팅방 생성, 참여, 목록 조회 |
| 서비스 | `AccountMyBatisService` | 계정 CRUD, 소속 모임 조회. MyBatis 경유 |
| 서비스 | `NoticeService` | 공지 CRUD 및 검색 |
| 데이터 접근 | `dao/repository/*` (JPA) | 엔티티 단위 CRUD, 채팅 내역 커서 조회 |
| 데이터 접근 | `dao/mybatis/*` (MyBatis) | 계정·소속 조회, 다중 테이블 조인, 삭제 |
| 데이터 접근 | `NoticeRepository4QueryDslImpl` | 공지 동적 검색 + DTO 프로젝션 |
| 인프라 | `ChatRoomRedisRepository` | 방 정보 캐시, 세션↔방 매핑, 접속자 수 |
| 인프라 | `RedisSubscriber` | Pub/Sub 수신 후 로컬 WebSocket 구독자에게 전달 |

> **영속성 기술 혼용** — 같은 `Account` 데이터에 JPA(`AccountRepository`)와 MyBatis(`AccountDao`) 두 경로가 병존합니다. 계정 조회·수정·삭제는 MyBatis, 모임/공지/채팅에서의 계정 참조는 JPA를 사용합니다. 학습 목적의 의도적 구성이나, **동일 데이터에 두 개의 진입점이 존재**하므로 변경 시 양쪽 정합성을 함께 검토해야 합니다.

## 3. 메시지 전달 구조

```
브라우저 ──WebSocket/STOMP──> 서버 인스턴스 ──Redis Pub/Sub──> 다른 인스턴스 ──> 브라우저
         (클라이언트 전송 구간)                 (인스턴스 간 전파 구간)

발행 경로
  ChatController.message()          JWT에서 발신자 확정
    └─ ChatService.sendChatMessage()
         ├─ (TALK) chatMessageRepository.save()   ← 영속화. 메시지당 1회
         └─ redisTemplate.convertAndSend()        채널 "chatroom"

수신 경로  (모든 인스턴스에서 실행)
  RedisSubscriber.sendMessage()
    └─ messagingTemplate.convertAndSend("/sub/chat/room/{roomId}")
         └─ SimpleBroker → 해당 인스턴스에 연결된 구독자
```

**설계 근거** — STOMP 브로커가 인메모리 `SimpleBroker`이므로 구독자 목록이 JVM 단위로만 존재합니다. Redis Pub/Sub이 인스턴스 간 격차를 메웁니다. 영속화를 **수신부가 아닌 발행부**에 둔 것은, 수신부에 두면 Pub/Sub이 모든 인스턴스에 전달하므로 서버 N대에서 동일 메시지가 N번 저장되기 때문입니다.

> **참고** — Redis와 WebSocket은 대체 관계가 아닙니다. WebSocket은 브라우저↔서버 전송 구간, Redis Pub/Sub은 서버 인스턴스 간 전파 구간을 담당하며 직렬로 연결됩니다. 브라우저는 Redis에 직접 연결할 수 없습니다.

## 4. 데이터 모델

| 엔티티 | 테이블 | 주요 컬럼 | 관계 · 제약 |
|---|---|---|---|
| `Account` | ACCOUNT | `account_id` PK · `email` UQ NN · `nick_name` · `password` NN · `roles` · `oauth2_id` UQ · `access_token` · `provider` | 1:N `AccountTeam` |
| `Team` | TEAM | `team_id` PK · `team_name` · `description` · `map_id` · `place_name` · `address_name` · `road_address_name` · `phone` · `place_url` · `x` · `y` · `created_at` · `updated_at` | 1:N `AccountTeam` (cascade ALL, orphanRemoval)<br>1:1 `ChatRoom` (cascade ALL) |
| `AccountTeam` | ACCOUNT_TEAM | `account_team_id` PK · `account_id` NN · `team_id` NN · `is_leader` NN | N:1 Account · N:1 Team |
| `ChatRoom` | **CHATROOM** | `roomId` PK (UUID, VARCHAR) · `roomName` | 1:1 Team. 테이블명 불일치 → [ISS-07](known-issues.md#iss-07) |
| `ChatMessage` | CHAT_MESSAGE | `chat_message_id` PK · `room_id` NN · `account_id` NULL · `sender_name` NN · `type` NN · `message` TEXT NN · `created_at` NN | N:1 ChatRoom · N:1 Account(nullable)<br>IDX `(room_id, chat_message_id)` |
| `Notice` | NOTICE | `id` PK · `title` NN · `content` TEXT · `account_id` | N:1 Account. IDX `title`, `account_id` |

- `Team`만 `BaseTimeEntity`를 상속해 생성·수정 시각을 가집니다. 다른 엔티티에는 시각 정보가 없습니다.
- `ChatMessage`는 상속 대신 자체 `created_at`과 `@PrePersist`를 사용합니다.
- `ChatMessage.account_id`가 nullable인 것은 계정 삭제 후에도 메시지를 보존하기 위함이며, 표시 이름은 `sender_name` 스냅샷으로 별도 유지합니다.

## 5. Redis 사용

| 키 / 채널 | 자료구조 | 용도 | 생명주기 |
|---|---|---|---|
| `CHAT_RECENT:{roomId}` | List (최대 101) | **방 진입 시 읽는 최신 메시지** | write-through(LPUSHX+LTRIM). TTL 1일, 접근 시 갱신 |
| `CHAT_AUTH:{roomId}:{oauth2Id}` | String | **멤버십 검증 결과** | TTL 5분. 탈퇴·강퇴 시 즉시 무효화 |
| `CHAT_ROOM` | Hash (field = roomId) | 방 정보 + 참여자 스냅샷 캐시 | 생성 시 1회 기록. **갱신·복구 경로 없음** → [ISS-04](known-issues.md#iss-04) |
| `ENTER_INFO` | Hash (field = sessionId) | WebSocket 세션 ↔ 방 매핑 | 구독 시 생성, 연결 종료 시 삭제 |
| `USER_COUNT_{roomId}` | String 카운터 | 방 접속자 수 | 입장 +1 / 퇴장 −1. 비정상 종료 시 누락 가능 |
| `chatroom` | Pub/Sub 채널 | 인스턴스 간 메시지 전파 | 휘발 |
| 세션 저장소 | Spring Session | 운영 프로필에서 HTTP 세션 공유 | 타임아웃 설정 미적용 → [ISS-08](known-issues.md#iss-08) |
| `noticeCacheManager` | 캐시 매니저 | 공지 조회 캐싱 **용도로 준비됨** | **사용처 없음** → [ISS-06](known-issues.md#iss-06) |

> Pub/Sub 채널이 **단일 채널**입니다. 모든 방의 메시지가 한 채널로 흐르므로, 각 인스턴스는 자신과 무관한 방의 메시지까지 수신·역직렬화합니다. 방 수가 늘수록 이 비용이 선형 증가합니다.

### 쿼리 분석

캐시를 넣기 전에 실제 비용을 측정했습니다. 코드를 읽어 세는 것과 실제 실행은 다를 수 있어, **Hibernate `Statistics`로 쿼리 수를 세고 `EXPLAIN ANALYZE`로 실행 계획을 확인**했습니다.

#### 쿼리 개수 (측정값)

`ChatHistoryQueryCountTest`가 이 수치를 회귀 테스트로 고정합니다.

| 상황 | 쿼리 수 | 내역 |
|---|---|---|
| 캐시 없음 | **4** | ① 방 조회(team 조인) ② 계정 조회 ③ 멤버십 조회 ④ 메시지 조회 |
| 멤버십만 캐시 | **1** | ④만 |
| 둘 다 캐시 | **0** | — |
| 스크롤 구간 | **1** | 첫 페이지만 캐싱하므로 항상 DB |

**4건 중 3건이 인가 검증**입니다. 메시지만 캐싱하면 4 → 3으로 줄 뿐이라, 멤버십까지 캐싱해야 의미 있는 개선이 됩니다.

#### 실행 계획 (H2, 방 하나에 메시지 234건)

| 쿼리 | 인덱스 사용 | scanCount |
|---|---|---|
| 커서 구간 (`before` 있음) | `IDX_CHAT_MESSAGE_ROOM_ID` 를 **범위 조건으로 사용** | **101** (조건 매칭 범위) |
| 첫 페이지 (`before` 없음) | 같은 인덱스로 방을 찾지만 정렬을 위해 전부 읽음 | **235** (방 전체) |

```
-- 커서 구간: 인덱스가 범위를 좁힌다
/* IDX_CHAT_MESSAGE_ROOM_ID: ROOM_ID = '...' AND CHAT_MESSAGE_ID < 100 */
/* scanCount: 101 */

-- 첫 페이지: WHERE 조건이 ROOM_ID뿐이라 방 전체를 읽고 정렬한다
/* IDX_CHAT_MESSAGE_ROOM_ID: ROOM_ID = '...' */
/* scanCount: 235 */
```

**첫 페이지 쿼리는 방 크기에 비례해 비용이 증가합니다.** 인덱스가 `ROOM_ID`로 방을 찾는 데까지는 쓰이지만, `ORDER BY id DESC LIMIT 51`을 만족시키려 그 방의 메시지를 전부 읽습니다. 그리고 이 첫 페이지가 바로 **방 진입 시 실행되는 쿼리**입니다. 캐싱 대상으로 적절한 이유가 여기 있습니다.

> ⚠️ **측정 환경의 한계** — 위 계획은 H2 기준입니다. MySQL은 같은 인덱스로 역방향 스캔을 하며 51건에서 조기 종료할 수 있어 결과가 다를 수 있습니다. 운영 환경에서 재측정이 필요합니다.
>
> 참고로 Hibernate가 FK용으로 `ROOM_ID` 단일 인덱스를 자동 생성해 두는데, 복합 인덱스의 접두사와 겹쳐 **중복**입니다. 첫 페이지 쿼리에서 H2가 이쪽을 고르기도 합니다.

### 방 진입 캐시

위 분석에 따라 **인가 검증과 첫 페이지 메시지를 함께 캐싱**합니다. 방 진입 시 DB 쿼리가 4 → 0이 됩니다.

| 원칙 | 내용 |
|---|---|
| 적용 범위 | **첫 페이지만** 캐시를 탄다. 위로 스크롤하는 구간은 항상 DB. 캐시/DB 경계가 `before` 유무 하나로 명확해져, 두 소스에 걸친 커서 계산을 피한다 |
| 용량 101 | 조회 size 상한이 100인데 다음 페이지 판정에 1건이 더 필요하다. 100이면 `size=100` 요청만 캐시를 못 타는 구멍이 생긴다 |
| LPUSHX | 키가 있을 때만 갱신한다. 키가 없는데 push하면 최신 몇 건만 든 반쪽짜리 캐시가 생겨, 이후 조회가 "더 이전 메시지가 없다"고 잘못 판단한다 |
| 멤버만 캐싱 | 비멤버까지 캐싱하면 방금 참여한 사용자가 TTL이 끝날 때까지 차단된다 |
| 즉시 무효화 | 탈퇴·강퇴는 권한을 **빼앗는** 작업이라 `PostTeamService.kickMember`에서 멤버십 캐시를 즉시 지운다. TTL 5분은 그 경로를 놓쳤을 때의 안전망 |

**실패 정책** — 캐시는 **가용성이 우선**이라 모든 Redis 오류를 삼키고 캐시 미스 또는 무동작으로 처리합니다. 메시지 영속화가 정합성을 우선해 저장 실패 시 발행까지 막는 것과 반대입니다. 예외가 전파되면 Redis 장애가 곧 채팅 조회 장애가 됩니다. 특히 **멤버십 조회 실패는 반드시 `false`를 반환**해야 합니다. `true`면 Redis 장애가 인가 우회로 이어집니다.

## 6. 인증 및 인가

| 구간 | 방식 | 구현 |
|---|---|---|
| HTTP | OAuth 2.0 로그인 + 서버 세션 | `CustomOAuth2UserService`가 provider별 응답을 정규화하고 계정을 생성·갱신 |
| HTTP (폼) | 이메일 + 비밀번호 | `CustomAuthenticationProvider` + BCrypt. 진입 경로 제한적 → [ISS-09](known-issues.md#iss-09) |
| WebSocket | JWT (HS256, 1시간) | STOMP CONNECT 시 `StompHandler`가 검증. 발행 시 토큰에서 발신자 확정 |
| 메서드 수준 | `@PreAuthorize` | 공지 쓰기 작업에 `hasRole('ROLE_ADMIN')` |
| 도메인 수준 | 멤버십 검증 | 채팅 내역 조회만 구현. 모임 관리 작업에는 없음 → [ISS-01](known-issues.md#iss-01) |

> ⚠️ **권한 문자열이 세 갈래로 갈려 있습니다.** 소셜 로그인은 `ROLE_USER`/`ROLE_ADMIN`을 부여하고 인가 검사도 이를 기준으로 하지만, 폼 회원가입은 `UserRole.USER_ROLE`(문자열 `"USER_ROLE"`)을 저장합니다. `UserRole` enum 자체도 `USER_ROLE, USER_ADMIN`으로 정의돼 어느 쪽과도 일치하지 않습니다. → [ISS-02](known-issues.md#iss-02)

## 7. 외부 연동

| 대상 | 용도 | 실패 시 영향 |
|---|---|---|
| 카카오·네이버·구글 OAuth | 로그인, 연동 해제 | 로그인 불가. 서비스 전면 이용 불가 |
| 카카오 지도 JS SDK | 장소 검색, 지도 표시 | 모임 개설 불가(장소 선택 단계에서 중단) |
| 카카오·네이버 로그아웃 API | 탈퇴 시 연동 해제 | 연동이 남아 재로그인 시 기존 계정으로 붙음 |

## 8. 실행 환경 및 구성

| 항목 | 개발 (dev) | 운영 (prod) |
|---|---|---|
| DB | H2 TCP `~/eatmate` | MySQL 8 (AWS RDS) |
| 스키마 | `ddl-auto: update` — 마이그레이션 도구 없음 → [ISS-10](known-issues.md#iss-10) | 동일 |
| Redis | `localhost` 기본값 | `REDIS_HOST` 필수 · Lettuce 풀 max-active 50 |
| 커넥션 풀 | 기본값 | Hikari 20 · Tomcat 스레드 50 (**잠정값**) |
| OAuth 키 | 더미 fallback (로컬 기동용) | 환경변수 필수 |
| 로깅 | `spring.logging.*`에 위치해 **바인딩되지 않음** → [ISS-08](known-issues.md#iss-08) | 동일 |
| 모니터링 | — | Actuator health · info · prometheus |

프로필 선택은 `SPRING_PROFILES_ACTIVE`로 하며 미지정 시 `dev`입니다. 실제 비밀값은 gitignore된 `config/application.yml`로 덮어쓰며, 이 파일은 클래스패스 설정보다 우선합니다.

## 9. 비기능 요구사항

| ID | 항목 | 기준 및 구현 | 상태 |
|---|---|---|---|
| NFR-01 | 수평 확장 | Redis Pub/Sub로 인스턴스 간 메시지 전파, 세션 공유. **실측 미검증**(단일 인스턴스 운영) | ⚠️ 부분 |
| NFR-02 | 내역 조회 성능 | 복합 인덱스 기반 커서 페이징. `count(*)` 미사용, `size+1`로 다음 페이지 판정 | ✅ 충족 |
| NFR-03 | 과다 조회 방어 | 내역 조회 `size` 상한 100 | ✅ 충족 |
| NFR-04 | 데이터 보존 | 대화는 RDBMS에 동기 저장. 저장 실패 시 발행도 중단(정합성 우선) | ✅ 충족 |
| NFR-05 | 비밀정보 관리 | 구조는 환경변수 기반. 소스에 하드코딩된 키 잔존 → [ISS-03](known-issues.md#iss-03) | ⚠️ 부분 |
| NFR-06 | 테스트 자동화 | 외부 의존 없는 슬라이스 테스트 28건. **채팅 도메인 한정** | ⚠️ 부분 |
| NFR-07 | 배포 재현성 | Dockerfile·CI·배포 스크립트 부재. 서버 수동 구성 | ❌ 미충족 |
| NFR-08 | 관측성 | Actuator + Prometheus 노출. 구조화 로깅 미적용 | ⚠️ 부분 |
| NFR-09 | 오류 처리 일관성 | 전역 예외 처리기 없음. 도메인 오류가 500으로 노출 | ❌ 미충족 |

---

관련 문서: [요구사항 명세](requirements.md) · [API 명세](api.md) · [알려진 이슈](known-issues.md)
