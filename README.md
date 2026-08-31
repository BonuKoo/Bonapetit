# EatMate — 실시간 채팅·지도 기반 식사 모임 매칭 서비스

<p align="center">
  <img src="src/main/resources/static/img/logo.png" alt="EatMate Logo" width="180"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.3-6DB33F" alt="Spring Boot 3.3.3"/>
  <img src="https://img.shields.io/badge/Redis-Pub%2FSub%20%C2%B7%20Cache-DC382D" alt="Redis"/>
  <img src="https://img.shields.io/badge/tests-50%20passing-0F6E63" alt="tests"/>
  <img src="https://img.shields.io/badge/known%20issues-12%20tracked-99610B" alt="known issues"/>
</p>

> 혼자 먹기 싫은 사람들을 위한 **식사 모임 매칭 플랫폼**.
> 모임 게시글을 올리면 전용 채팅방이 열리고, 카카오맵으로 만날 장소를 정하는 흐름을 하나의 Spring Boot 애플리케이션으로 구현했습니다.

---

## 이 저장소에서 한 일

이 프로젝트는 **1년 가까이 방치되어 애플리케이션이 부팅조차 되지 않는 상태**에서 다시 시작했습니다.

| 영역 | 시작 상태 | 현재 |
|---|---|---|
| 애플리케이션 | `application.yml`에 병합 충돌 마커가 남아 **부팅 불가** | 정상 기동 |
| 채팅 메시지 | **어디에도 저장되지 않음** — 내역 조회가 원천적으로 불가능 | RDBMS 영속화 + 커서 조회 + Redis 캐시 |
| 스키마 수명 | `ddl-auto: create-drop` — 재시작마다 전체 삭제 | `update` 복구 |
| 테스트 | `contextLoads()`조차 실패, 실행에 외부 의존 3종 필요 | **50건 통과**, 외부 의존 0 |
| 운영 시크릿 | RDS 계정·OAuth 키 평문 하드코딩 | 환경변수화 |
| 문서 | 없음 (커밋되지 않은 README 하나) | 명세 4종 + 작업 기록 + 인수인계 |

```
쿼리      방 진입 4건 → 0건        인가 검증 3건을 캐시로 제거
인덱스    1,016행 → 51행           0.623 ms → 0.116 ms
검증      실사용 234건             커서 경계 중복·누락 0
테스트    0건 → 50건               외부 의존 없이 실행
```

전체 이력은 [작업 기록](docs/worklog-2026-08.md)에, 아직 남은 결함은 [알려진 이슈](docs/known-issues.md)에 있습니다.

---

## 판단의 기록

기능보다 **왜 그렇게 결정했는지**를 남기는 데 무게를 뒀습니다.

### 개발 DB만 보고 결론 냈다가 뒤집은 이야기

방 진입 속도를 개선하려 캐시를 넣기 전에, 먼저 무엇이 비용인지 측정했습니다. 개발용 H2에서 `EXPLAIN ANALYZE`를 돌린 결과는 이랬습니다.

```
-- H2 · 첫 페이지 (방에 메시지 234건)
/* IDX_CHAT_MESSAGE_ROOM_ID: ROOM_ID = '...' */
/* scanCount: 235 */          ← 방의 메시지를 전부 읽고 정렬
```

여기서 **"첫 페이지 조회가 방 크기에 비례해 느려진다"**고 결론 내고, 그것을 메시지 캐싱의 근거로 삼았습니다.

개발 DB만 보고 판단하는 게 위험하다고 판단해 **MySQL 8.4에 동일 스키마를 만들고 11만 건으로 재측정**했습니다. 결과가 갈렸습니다.

```
-- MySQL · 같은 쿼리 (방에 메시지 5,000건 / 테이블 전체 11만 건)
-> Limit: 51 row(s)  (actual time=0.017..0.107 rows=51 loops=1)
    -> Index lookup using idx_chat_message_room_id (room_id='bulk-7') (reverse)
       (cost=1053 rows=5000) (actual rows=51 loops=1)
```

MySQL은 복합 인덱스를 **역방향으로 스캔하며 `LIMIT`에서 조기 종료**합니다. 방 크기와 무관하게 51행만 읽습니다. H2에서 본 전체 스캔은 **H2 옵티마이저의 한계**였지 쿼리 설계 문제가 아니었습니다.

**앞선 결론을 철회하고 문서를 정정했습니다.** 캐시의 실질적 가치는 메시지 조회 가속이 아니라 **인가 검증 3쿼리 제거**에 있습니다. 정정 이력은 지우지 않고 [작업 기록 6.3절](docs/worklog-2026-08.md)에 남겼습니다.

한편 복합 인덱스 자체는 값을 합니다.

| 조건 | 실제 읽은 행 | 시간 |
|---|---|---|
| 복합 인덱스 사용 | **51** | 0.116 ms |
| `PRIMARY`만 사용 | **1,016** | 0.623 ms |

인덱스가 없으면 PK를 역방향으로 훑으며 `room_id`가 맞지 않는 행을 계속 버립니다. 51건을 얻으려 1,016행을 읽고 965행을 폐기합니다.

### 무엇이 비용인지부터 셌습니다

캐시 대상을 고르기 전에 Hibernate `Statistics`로 실제 쿼리 수를 측정했습니다.

| 상황 | 쿼리 수 |
|---|---|
| 캐시 없음 | **4** (방 조회 · 계정 조회 · 멤버십 조회 · 메시지 조회) |
| 멤버십만 캐시 | **1** |
| 둘 다 캐시 | **0** |

**4건 중 3건이 인가 검증**이었습니다. 메시지만 캐싱하면 4 → 3으로 줄 뿐이라 멤버십까지 캐싱했습니다. `ChatHistoryQueryCountTest`가 이 수치를 회귀 테스트로 고정합니다 — 페치 전략이나 인가 로직이 바뀌어 쿼리가 늘면 거기서 걸립니다.

### 저장 지점을 발행부에 둔 이유

메시지 영속화 위치는 두 곳이 후보였습니다.

| 위치 | 결과 |
|---|---|
| `RedisSubscriber` (수신부) | Pub/Sub이 **모든 인스턴스**에 전달하므로 서버 N대에서 같은 메시지가 **N번 저장** |
| `ChatService` (발행부) | 메시지당 **정확히 1회** 실행 |

발행부를 택하고 `@Transactional`로 묶어 **저장 실패 시 발행도 중단**되게 했습니다. 화면에는 보이는데 내역에는 없는 메시지를 만들지 않기 위해 정합성을 우선했습니다.

동기 INSERT를 택한 근거는, 비동기는 서버 다운 시 버퍼가 유실되고 Redis 버퍼링은 조회가 "아직 내려가지 않은 분량 + DB 분량"에 걸쳐 **커서 페이징이 두 데이터 소스로 쪼개지기** 때문입니다. 병목으로 측정된 바 없는 상태에서 도입하면 잃는 게 큽니다.

### 캐시는 가용성, 영속화는 정합성

같은 Redis를 쓰지만 실패 정책을 **의도적으로 반대**로 설계했습니다.

| | 실패 시 |
|---|---|
| 메시지 영속화 | 예외 전파 → **발행까지 중단** (정합성 우선) |
| 방 진입 캐시 | 예외 삼킴 → **DB로 폴백** (가용성 우선) |

캐시 예외가 전파되면 Redis 장애가 곧 채팅 조회 장애가 됩니다. 특히 **멤버십 조회 실패는 반드시 `false`를 반환**해야 합니다 — `true`면 Redis 장애가 인가 우회로 이어집니다. 이 계약을 테스트 6건으로 고정했습니다.

### 커서 페이징 — `Page` 대신 `List`

```java
@Query("select m from ChatMessage m left join fetch m.sender " +
       "where m.chatRoom.roomId = :roomId and m.id < :beforeId " +
       "order by m.id desc")
List<ChatMessage> findByRoomIdBefore(...);
```

- **정렬 키는 `id`** — `createdAt`은 같은 밀리초에 여러 건이 들어올 수 있어 커서로 부적합
- **`Page` 대신 `List` + `Pageable`** — `Page`는 매 요청마다 `count(*)`를 추가 실행. 채팅 내역에 전체 건수는 불필요하고, `size+1`건 조회로 다음 페이지 존재 여부를 판정하면 충분
- **경계는 배타적(`<`)** — `<=`로 두면 페이지마다 1건씩 중복

실사용 234건으로 경계를 검증했습니다.

```
page1: ID 234..185  50건  hasMore=true   page4: ID 84..35  50건  hasMore=true
page2: ID 184..135  50건  hasMore=true   page5: ID 34..1   34건  hasMore=false
page3: ID 134..85   50건  hasMore=true   합계 234건 — 중복·누락 0
```

### 설정 프로필 분리

단일 `application.yml` 안에서 `---` 문서 겹침으로 환경을 나누던 구조를 **공통 / dev / prod 3파일로 분리**했습니다. 기존 구조는 브랜치마다 같은 파일이 다르게 진화해 병합 충돌이 반복됐고 — 실제로 1년 방치된 충돌의 원인이었습니다 — 운영 문서가 기본 문서 위에 얹히면서 **개발용 설정이 운영으로 새는** 문제도 있었습니다.

분리 후 원본과 프로퍼티 단위로 대조해 동작 동등성을 확인했습니다(dev 차이 0건, prod 차이 4건 — 모두 바인딩되지 않는 죽은 키).

---

## 시스템 개요

OAuth 소셜 로그인으로 진입한 사용자가 식사 모임을 만들고 참여하며, 모임이 결성되면 **WebSocket(STOMP) + Redis Pub/Sub 기반 실시간 채팅방**에서 소통하고, **카카오맵 SDK**로 약속 장소를 지정합니다.

**단일 서버 모놀리식**이며, 하나의 애플리케이션 안에서 인증(OAuth2 + JWT) · 모임 도메인 · 실시간 채팅 · 지도 · 공지의 계층형 아키텍처를 다루는 데 초점을 맞췄습니다.

| 구분 | 기술 | 역할 |
|---|---|---|
| **Language / Framework** | Java 17, Spring Boot 3.3.3 | 단일 모놀리식 서버 |
| **Real-time** | Spring WebSocket + STOMP, Redis Pub/Sub | 채팅 전달. Pub/Sub은 **인스턴스 간 전파** 담당 |
| **In-Memory** | Redis (Lettuce) | 세션 스토어 · **방 진입 캐시** · 채팅방 메타데이터 · 세션↔방 매핑 · 접속자 수 · Pub/Sub |
| **Database** | MySQL 8 (AWS RDS) / H2 (로컬 TCP) | 계정 · 모임 · **대화 내역** · 공지 |
| **ORM / Query** | Spring Data JPA, QueryDSL 5.0, MyBatis 3.0 | JPA 주력 + 동적 검색 + 계정 도메인 MyBatis 병행 |
| **Auth** | Spring Security, OAuth2 Client, JWT (jjwt) | 소셜 로그인 3종, STOMP CONNECT 시 토큰 검증 |
| **Map** | Kakao Maps JS SDK | 키워드 장소 검색 · 마커 · 좌표 수집 |
| **View** | Thymeleaf (+Layout Dialect) + Vue 2 | 서버 렌더링(MPA) + 채팅 화면 Vue |
| **Monitoring** | Actuator, Micrometer, Prometheus | 헬스체크 및 메트릭 |

### 아키텍처

```mermaid
flowchart TB
    subgraph Client["클라이언트 (브라우저)"]
        UI["Thymeleaf MPA<br/>+ Vue 2 (채팅)"]
    end

    subgraph EC2["AWS EC2 · Spring Boot 3.3.3 (단일 모놀리식)"]
        direction TB
        SEC["Security · OAuth2 · JWT<br/>인증/인가 필터 체인"]
        WEB["Controller 계층<br/>Account·Post·Team·Notice·Map"]
        WS["WebSocket / STOMP<br/>StompHandler (JWT 검증)"]
        SVC["Service 계층<br/>JPA(+QueryDSL) · MyBatis"]
    end

    subgraph Data["데이터 저장소"]
        REDIS[("Redis<br/>세션·캐시·Pub/Sub")]
        DB[("MySQL RDS / H2<br/>계정·모임·대화내역·공지")]
    end

    subgraph Ext["외부 서비스"]
        OAUTH["OAuth Provider<br/>카카오·네이버·구글"]
        MAP["Kakao Maps JS SDK"]
    end

    UI -->|HTTP| SEC
    UI -.->|STOMP /ws-stomp| WS
    UI -.->|장소 검색| MAP
    SEC --> WEB
    WEB --> SVC
    WS --> SVC
    SVC --> DB
    WS <-->|Pub/Sub chatroom| REDIS
    SEC <-->|세션| REDIS
    SEC -->|인가 코드 교환| OAUTH
```

### 메시지 전달 경로

```
[Vue 클라이언트] --STOMP CONNECT/SUBSCRIBE/SEND--> [StompHandler] --JWT 검증
        │ /pub/chat/message
        ▼
[ChatService.sendChatMessage()]
        ├─ (TALK) chatMessageRepository.save()     ← RDBMS 동기 영속화
        ├─         chatCacheRepository.pushRecent() ← 최신 목록 캐시 갱신
        └─ redisTemplate.convertAndSend()           ← Redis Pub/Sub "chatroom"
                 │
                 ▼  (모든 인스턴스가 수신)
        [RedisSubscriber] --SimpMessagingTemplate--> /sub/chat/room/{roomId}
```

<details>
<summary><b>계층 구조와 도메인 모델</b></summary>

```
Config (Security / DB / WebSocket / Redis / QueryDSL)
   │
Security · OAuth2 · JWT ── 인증/인가 필터 체인
   │
Controller (Account · Post · Team · Chat · Notice · Map)
   │
Service ── ChatService(쓰기) / ChatHistoryService(읽기) 분리
   │        JPA 주력 + 계정 도메인 MyBatis 병행
DAO ── ┌ JPA Repository (+ QueryDSL: Team · Notice · ChatMessage)
        └ MyBatis @Mapper (Account · AccountTeam)
   │
DataSource ── H2(로컬 TCP) / MySQL(운영 RDS)  ·  Redis
```

- **JPA + MyBatis 하이브리드**: `DBConfig`가 하나의 `DataSource`를 공유하며 둘을 동시에 구성합니다. 신규 기능은 JPA(+QueryDSL) 우선.
- **채팅 도메인 읽기/쓰기 분리**: 인가 검증과 커서 계산이 붙으면서 한 클래스가 과하게 커져 `ChatService`(발행·영속화)와 `ChatHistoryService`(조회·인가)로 나눴습니다.

```
Account ──< AccountTeam >── Team ──1:1── ChatRoom ──< ChatMessage
   │                                       (UUID PK)      (커서 페이징)
   └──< Notice
```

| 엔티티 | 설명 |
|---|---|
| `Account` | 사용자 계정 (`email` · `oauth2_id` unique, `provider` · `roles`) |
| `Team` | 식사 모임. 지도 필드(`placeName` · `addressName` · `x` · `y` 등), `BaseTimeEntity` 상속 |
| `AccountTeam` | 계정↔모임 조인 엔티티 (`isLeader` 개설자 여부) |
| `ChatRoom` | 모임과 1:1, UUID 문자열 PK |
| `ChatMessage` | 대화 내역. `sender_name` 스냅샷, 인덱스 `(room_id, chat_message_id)` |
| `Notice` | 관리자 공지 (`title` 인덱스, `content` TEXT) |

`ChatMessage.sender`는 nullable입니다. 계정이 삭제돼도 메시지는 남아야 하며, 표시 이름은 **발신 시점 스냅샷**으로 별도 보존해 닉네임 변경 시에도 과거 대화의 발신자가 바뀌지 않습니다.

</details>

---

## 테스트

외부 의존(H2 서버 · Redis · 환경변수) 없이 실행되는 슬라이스 테스트 **50건**입니다.

```bash
./gradlew test
```

| 계층 | 방식 | 건수 | 중점 |
|---|---|---|---|
| 저장소 | `@DataJpaTest` | 6 | 커서 경계(중복·누락), 방 격리, `hasMore` 판정 |
| `ChatCacheRepository` | Mockito | 10 | **Redis 장애 격리**, LPUSHX 조건부 갱신, 통째 교체 |
| `ChatService` | Mockito | 8 | TALK만 저장, 저장 실패 시 발행 차단, 캐시 반영 |
| `ChatHistoryService` | Mockito | 17 | 커서 · 정렬 반전 · size 클램프 · 인가 · 캐시 히트/미스 |
| **쿼리 수 측정** | `@DataJpaTest` + Hibernate `Statistics` | 4 | 방 진입 쿼리 4 → 1 → 0 회귀 방지 |
| 컨트롤러 | `@WebMvcTest` | 4 | 응답 JSON, 파라미터 바인딩, 403 |
| 컨텍스트 | `@SpringBootTest` | 1 | 기동 |

테스트 인프라 자체가 이 작업에서 처음 생겼습니다. 기존 `contextLoads()`는 H2 TCP 서버에 붙지 못해 실패하고 있었고, 실행에 H2 · Redis · 환경변수 6개가 모두 필요했습니다.

자동 테스트는 현재 **채팅 도메인에 집중**되어 있습니다. 계정 · 모임 · 공지 도메인은 자동 검증이 없으며 [알려진 이슈](docs/known-issues.md)에 기록해 두었습니다.

---

<details>
<summary><b>기능 목록</b></summary>

### 인증 및 보안
- **OAuth 2.0 소셜 로그인 3종** (카카오 · 네이버 · 구글) — `CustomOAuth2UserService`가 provider별 응답 구조를 정규화하고 최초 로그인 시 계정을 자동 생성
- **WebSocket 구간 JWT 인증** — STOMP CONNECT 시 토큰 검증, 발행 시 **토큰에서 발신자를 확정**. 클라이언트가 보낸 발신자 정보는 신뢰하지 않음

### 모임 관리
- **게시글 작성 = 모임 + 채팅방 동시 생성** — `Team`과 1:1로 연결된 `ChatRoom`이 함께 생성되고 작성자가 개설자로 등록
- **참여 / 탈퇴 / 강퇴** — `AccountTeam` 조인 엔티티로 멤버십 관리, 중복 참여는 서비스 계층에서 차단
- **검색** — 모임명 · 장소명 · 주소로 페이징 검색

### 실시간 채팅 + 대화 내역
- 대화가 **RDBMS에 영구 보관**되어 재접속 · 새로고침 · 서버 재시작 후에도 유지
- 진입 시 최근 50건을 불러오고 **위로 스크롤하면 커서 기반으로 이전 대화를 이어서** 로드
- 입장 · 퇴장 알림은 실시간으로만 표시하고 내역에는 남기지 않음

### 지도 통합
- 키워드 장소 검색 · 마커 렌더링 · 현재 위치 기준 초기 중심좌표
- 장소명 · 주소 · 좌표 · 전화번호를 `Team` 엔티티에 매핑

### 공지
- 관리자(`ROLE_ADMIN`) 전용 CRUD, QueryDSL 동적 검색 + DTO 프로젝션 페이징

</details>

---

## 로컬 실행

**사전 요구사항**: JDK 17, Docker(또는 Redis), H2 실행 파일

```bash
# 1) Redis
docker run -d --name eatmate-redis -p 6379:6379 redis

# 2) H2 TCP 서버 — -ifNotExists 필수 (H2 2.x는 없으면 원격 DB 생성을 거부)
java -cp <h2.jar> org.h2.tools.Server -tcp -ifNotExists -web

# 3) 애플리케이션
./gradlew bootRun
```

환경변수 없이 `dev` 프로필로 기동합니다. 소셜 로그인과 지도를 쓰려면 실제 키가 필요하며, **`config/application.yml`**(gitignore 대상)에 넣으면 클래스패스 설정보다 우선 적용됩니다.

```yaml
# config/application.yml — 커밋되지 않습니다
kakao:
  client:
    id: 발급받은_REST_API_키
    secret: 발급받은_Client_Secret
```

운영 프로필은 `SPRING_PROFILES_ACTIVE=prod`와 함께 `DB_URL` · `DB_USERNAME` · `DB_PASSWORD` · `REDIS_HOST` · `JWT_SECRET` · OAuth 키 6종을 환경변수로 요구합니다.

---

## 문서

| 문서 | 내용 |
|---|---|
| [요구사항 명세](docs/requirements.md) | 사용자 요구사항 14건, 인수 기준, 추적성 매트릭스 |
| [시스템 명세](docs/architecture.md) | 구성 요소 · 데이터 모델 · Redis 사용 · 쿼리 분석 · 캐시 설계 |
| [API 명세](docs/api.md) | HTTP 엔드포인트 41건 + STOMP 채널 3건 |
| [알려진 이슈](docs/known-issues.md) | 식별된 결함 12건과 조치 우선순위 |
| [작업 기록](docs/worklog-2026-08.md) | 작업 전체 이력과 검증 데이터 |
| [이어가기 문서](docs/handoff.md) | 새 작업자가 바로 이어받을 수 있는 인수인계 |

이 프로젝트는 완성품이 아니라 **개선 중인 시스템**입니다. 그래서 잘 된 것뿐 아니라 **아직 잘못된 것도 함께 문서화**했습니다.

- 명세서의 각 요구사항에는 실제 구현 상태(충족 / 부분 / 미충족)를 표기했습니다
- 스스로 식별한 결함 12건을 심각도와 조치 순서까지 정리해 [별도 문서](docs/known-issues.md)로 관리합니다. 가장 시급한 것은 **인가 검증 누락(ISS-01)** 입니다
- 측정 결과로 판단이 뒤집힌 이력도 지우지 않고 남겼습니다

문서는 완성된 코드를 정독해 역으로 정리한 **as-built 명세**입니다.

---

## 개발자

| 이름 | GitHub |
|---|---|
| 구본우 | [@BonuKoo](https://github.com/BonuKoo) |
