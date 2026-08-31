# EatMate — 실시간 채팅·지도 기반 식사 모임 매칭 서비스

<p align="center">
  <img src="src/main/resources/static/img/logo.png" alt="EatMate Logo" width="180"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.3-6DB33F" alt="Spring Boot 3.3.3"/>
  <img src="https://img.shields.io/badge/Redis-Pub%2FSub%20%C2%B7%20Cache-DC382D" alt="Redis"/>
  <img src="https://img.shields.io/badge/tests-50%20passing-0F6E63" alt="tests"/>
</p>

---

## 개요

혼자 먹기 싫은 사람들을 위한 **식사 모임 매칭 플랫폼**입니다.

소셜 계정으로 로그인한 사용자가 모임 게시글을 올리면 **전용 채팅방이 함께 생성**되고, 다른 사용자가 참여해 실시간으로 대화하며, **카카오맵**으로 만날 장소를 정하는 흐름을 하나의 Spring Boot 애플리케이션으로 구현했습니다.

| 항목 | 내용 |
|---|---|
| 형태 | 단일 모놀리식 웹 애플리케이션 (서버 사이드 렌더링 + 부분 SPA) |
| 핵심 도메인 | 인증(OAuth2 + JWT) · 모임 · 실시간 채팅 · 지도 · 공지 |
| 실시간 통신 | WebSocket(STOMP) + Redis Pub/Sub — 다중 인스턴스 확장 대비 |
| 데이터 | MySQL(운영) / H2(개발) + Redis(세션 · 캐시 · Pub/Sub) |

---

## 전체 아키텍처

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

### 연관 시스템

별도 저장소로 분리된 하위 프로젝트는 없습니다. 단일 저장소에 도메인별 패키지로 구성되며, 아래 외부 시스템과 연동합니다.

| 시스템 | 역할 | 장애 시 영향 |
|---|---|---|
| **카카오 · 네이버 · 구글 OAuth** | 소셜 로그인, 탈퇴 시 연동 해제 | 로그인 불가 → 서비스 전면 이용 불가 |
| **Kakao Maps JS SDK** | 키워드 장소 검색, 마커 렌더링, 좌표 수집 | 모임 개설 불가 (장소 선택 단계에서 중단) |
| **Redis** | 세션 스토어 · 방 진입 캐시 · 인스턴스 간 메시지 전파 · 접속자 수 | 세션 소실. 채팅 조회는 DB 폴백으로 동작 |
| **MySQL / H2** | 계정 · 모임 · 대화 내역 · 공지 영속화 | 서비스 중단 |

---

## 도메인별 상세 역할

`src/main/java/com/eatmate` 아래 도메인 패키지로 나뉩니다.

| 패키지 | 역할 | 주요 구성 |
|---|---|---|
| **`chat`** | 실시간 채팅과 대화 내역. 이 저장소에서 가장 밀도가 높은 도메인 | `ChatService`(발행·영속화) / `ChatHistoryService`(조회·인가) / `ChatCacheRepository`(Redis 캐시) / `StompHandler`(JWT 검증) / `RedisSubscriber`(Pub/Sub 수신) |
| **`post` · `team`** | 모임 게시글과 팀 멤버십. 게시글 작성이 곧 모임·채팅방 생성 | `PostJpaService`(모임+채팅방 동시 생성) / `TeamJpaService`(참여·목록) / `PostTeamService`(강퇴·탈퇴) |
| **`account`** | 계정 CRUD와 프로필. 유일하게 MyBatis를 주로 쓰는 도메인 | `AccountMyBatisService` / `AccountProfileController` |
| **`oauth` · `security` · `jwt`** | 인증/인가 전반 | `CustomOAuth2UserService`(provider별 응답 정규화) / `SecurityConfig` / `JwtTokenProvider` |
| **`notice`** | 관리자 공지 CRUD와 검색 | `NoticeService` / QueryDSL 동적 검색 |
| **`map`** | 카카오맵 장소 검색 화면 | `MapVo`(좌표·주소를 모임에 매핑) |
| **`dao`** | 데이터 접근. JPA와 MyBatis가 공존 | `dao/repository`(JPA + QueryDSL) / `dao/mybatis`(`@Mapper`) |
| **`db` · `redis`** | 인프라 설정 | `DBConfig`(DataSource·JPA·MyBatis 동시 구성) / `RedisConfig`(Pub/Sub·템플릿) |

### 계층 구조

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

### 도메인 모델

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

---

## 주요 기능

### 인증 및 보안
- **OAuth 2.0 소셜 로그인 3종** (카카오 · 네이버 · 구글). provider별 응답 구조를 정규화하고 최초 로그인 시 계정 자동 생성
- **WebSocket 구간 JWT 인증** — STOMP CONNECT 시 토큰 검증, 발행 시 토큰에서 발신자를 확정. 클라이언트가 보낸 발신자 정보는 신뢰하지 않음

### 모임
- **게시글 작성 = 모임 + 채팅방 동시 생성.** 작성자가 개설자로 등록
- **지도 기반 장소 지정** — 키워드 검색으로 고른 장소의 이름 · 주소 · 좌표 · 전화번호를 모임에 저장
- **참여 / 탈퇴 / 강퇴** — 중복 참여는 서비스 계층에서 차단
- **검색** — 모임명 · 장소명 · 주소로 페이징 검색

### 실시간 채팅
- 모임 전용 채팅방에서 실시간 대화. **다중 서버 인스턴스에서도 메시지 누락 없음**
- 입장 · 퇴장 알림과 현재 접속자 수 표시

### 대화 내역
- 대화가 **RDBMS에 영구 보관**되어 재접속 · 새로고침 · 서버 재시작 후에도 유지
- 진입 시 최근 50건 로드, **위로 스크롤하면 커서 기반으로 이전 대화를 이어서** 로드
- 입장 · 퇴장 알림은 실시간으로만 표시하고 내역에는 남기지 않음
- **해당 모임의 참여자만** 내역 조회 가능

### 공지
- 관리자(`ROLE_ADMIN`) 전용 CRUD, QueryDSL 동적 검색 + DTO 프로젝션 페이징

---

## 기술 스택

| 구분 | 기술 | 역할 |
|---|---|---|
| **Language / Framework** | Java 17, Spring Boot 3.3.3 | 단일 모놀리식 서버 |
| **Real-time** | Spring WebSocket + STOMP, SockJS, Redis Pub/Sub | 채팅 전달. Pub/Sub은 **인스턴스 간 전파** 담당 |
| **In-Memory** | Redis (Lettuce) | 세션 스토어 · 방 진입 캐시 · 채팅방 메타데이터 · 세션↔방 매핑 · 접속자 수 · Pub/Sub |
| **Database** | MySQL 8 (AWS RDS) / H2 (로컬 TCP) | 계정 · 모임 · 대화 내역 · 공지 |
| **ORM / Query** | Spring Data JPA, QueryDSL 5.0, MyBatis 3.0 | JPA 주력 + 동적 검색 + 계정 도메인 MyBatis 병행 |
| **Auth** | Spring Security, OAuth2 Client, JWT (jjwt 0.11.5) | 소셜 로그인 3종, STOMP CONNECT 시 토큰 검증 |
| **Map** | Kakao Maps JS SDK | 장소 검색 · 마커 · 좌표 수집 |
| **View** | Thymeleaf (+Layout Dialect), Vue 2, Bootstrap 4 | 서버 렌더링(MPA) + 채팅 화면 Vue |
| **Monitoring** | Actuator, Micrometer, Prometheus | 헬스체크 및 메트릭 |
| **Test** | JUnit 5, Mockito, Spring Security Test | 50건 — 저장소 · 서비스 · 컨트롤러 슬라이스 |
| **Build** | Gradle | 의존성 관리 및 빌드 |

---

## 디렉토리 구조

```
eatmate/
├── README.md
├── build.gradle              Gradle 빌드 설정
├── settings.gradle
├── config/                   로컬 전용 설정 (gitignore) — 실제 OAuth 키를 여기에
├── docs/                     명세·의사결정·작업기록
│   ├── requirements.md         요구사항 명세
│   ├── architecture.md         시스템 명세
│   ├── api.md                  API 명세
│   ├── decisions.md            의사결정 기록 (ADR)
│   ├── testing.md              테스트 전략
│   ├── known-issues.md         알려진 이슈
│   ├── worklog-2026-08.md      작업 기록
│   └── handoff.md              인수인계
└── src/
    ├── main/
    │   ├── java/com/eatmate/
    │   │   ├── account/        계정 CRUD · 프로필 (MyBatis 주력)
    │   │   ├── chat/           실시간 채팅 · 대화 내역 · 캐시
    │   │   │   ├── config/       WebSocketConfig
    │   │   │   ├── controller/   ChatController · ChatRoomController
    │   │   │   ├── dto/          ChatMessageDTO · ChatHistoryResponse 등
    │   │   │   ├── handler/      StompHandler (JWT 검증)
    │   │   │   ├── pubsub/       RedisSubscriber
    │   │   │   ├── redisDao/     ChatCacheRepository · ChatRoomRedisRepository
    │   │   │   └── service/      ChatService(쓰기) · ChatHistoryService(읽기)
    │   │   ├── dao/            데이터 접근
    │   │   │   ├── mybatis/      @Mapper (Account · AccountTeam · Notice)
    │   │   │   └── repository/   JPA Repository (+ QueryDSL impl)
    │   │   ├── db/             DBConfig · QueryDslConfig · JpaAuditingConfig
    │   │   ├── domain/         엔티티 · DTO · 상수
    │   │   ├── jwt/            JwtTokenProvider · JwtController
    │   │   ├── map/            MapVo
    │   │   ├── notice/         공지 CRUD
    │   │   ├── oauth/          CustomOAuth2UserService · provider별 UserInfo
    │   │   ├── post/           모임 게시글
    │   │   ├── redis/          RedisConfig · RedisCacheConfig
    │   │   ├── security/       SecurityConfig · Provider · Handler
    │   │   ├── team/           팀 참여 · 목록
    │   │   └── weblogout/      제공자별 로그아웃
    │   └── resources/
    │       ├── application.yml          공통 설정
    │       ├── application-dev.yml      개발 프로필
    │       ├── application-prod.yml     운영 프로필
    │       ├── static/                  JS · CSS · 이미지
    │       └── templates/               Thymeleaf (account · chat · map · notice · post)
    └── test/
        ├── java/com/eatmate/            테스트 50건
        └── resources/
            └── application-test.yml     테스트 프로필 (오버레이)
```

---

## 개발 환경 세팅 & 실행

### 사전 요구사항

| 항목 | 버전 · 비고 |
|---|---|
| JDK | 17 |
| Docker | Redis 실행용 (직접 설치한 Redis도 무방) |
| H2 | 실행 파일. 애플리케이션이 쓰는 버전(2.2.224)과 맞출 것 |

### 실행

```bash
# 1) Redis
docker run -d --name eatmate-redis -p 6379:6379 redis

# 2) H2 TCP 서버 — -ifNotExists 필수 (H2 2.x는 없으면 원격 DB 생성을 거부)
java -cp <h2.jar> org.h2.tools.Server -tcp -ifNotExists -web

# 3) 애플리케이션
./gradlew bootRun
```

**환경변수 없이 `dev` 프로필로 기동합니다.** 접속은 http://localhost:8080 입니다.

### 실제 OAuth 키 설정

소셜 로그인과 지도를 쓰려면 실제 키가 필요합니다. **`config/application.yml`** 에 넣으면 됩니다. 이 파일은 gitignore 대상이며, Spring Boot가 클래스패스 설정보다 높은 우선순위로 자동 로드하므로 IDE에서 그냥 Run 해도 적용됩니다.

```yaml
# config/application.yml — 커밋되지 않습니다
kakao:
  client:
    id: 발급받은_REST_API_키
    secret: 발급받은_Client_Secret
```

카카오 콘솔에서 **플랫폼 Web 사이트 도메인**(`http://localhost:8080`)과 **Redirect URI**(`http://localhost:8080/login/oauth2/code/kakao`)를 등록해야 하고, 로그인 **동의항목에 닉네임**이 켜져 있어야 합니다.

### 테스트

```bash
./gradlew test
```

외부 의존(H2 서버 · Redis · 환경변수) 없이 실행됩니다. 자세한 구성은 [테스트 전략](docs/testing.md)을 참조하세요.

### 실행 시 알아둘 것

| 증상 | 원인과 대응 |
|---|---|
| `bootRun` 중단 후 포트 충돌 | Gradle 데몬의 자식 java가 8080을 계속 점유합니다. `Get-NetTCPConnection -LocalPort 8080` 으로 PID를 찾아 종료 |
| H2 콘솔에서 한글이 깨져 보임 | H2 Shell 출력이 cp949라 그렇고 **DB 저장은 정상**입니다. `SELECT RAWTOHEX(STRINGTOUTF8(col))` 로 확인 |
| H2 연결 실패 | TCP 서버에 `-ifNotExists` 를 붙였는지 확인 |

---

## 빌드 및 배포

### 빌드

```bash
./gradlew clean build          # 테스트 포함
./gradlew bootJar              # 실행 가능한 JAR 생성
```

산출물은 `build/libs/` 아래 생성됩니다.

### 운영 실행

`prod` 프로필은 아래 환경변수를 **모두** 요구합니다. 하나라도 빠지면 기동에 실패합니다(fallback 없음).

| 환경변수 | 용도 |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | 프로필 선택 |
| `DB_URL` · `DB_USERNAME` · `DB_PASSWORD` | MySQL 접속 |
| `REDIS_HOST` | Redis 호스트 |
| `JWT_SECRET` | WebSocket 인증 토큰 서명 |
| `KAKAO_CLIENT_ID` · `KAKAO_CLIENT_SECRET` | 카카오 OAuth |
| `NAVER_CLIENT_ID` · `NAVER_CLIENT_SECRET` | 네이버 OAuth |
| `GOOGLE_CLIENT_ID` · `GOOGLE_CLIENT_SECRET` | 구글 OAuth |

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_URL=... DB_USERNAME=... DB_PASSWORD=... \
REDIS_HOST=... JWT_SECRET=... \
java -jar build/libs/eatmate-0.0.1-SNAPSHOT.jar
```

### 운영 구성

AWS EC2 위에서 Nginx 리버스 프록시를 두고 단일 인스턴스로 동작합니다. `prod` 프로필에 `server.forward-headers-strategy: framework` 가 설정되어 `X-Forwarded-*` 헤더를 신뢰합니다.

Actuator가 `health` · `info` · `prometheus` 를 노출합니다.

> ⚠️ **배포 자동화가 없습니다.** Dockerfile · CI 워크플로 · 배포 스크립트가 저장소에 없어 서버 구성이 재현되지 않습니다. 스키마도 마이그레이션 도구 없이 `ddl-auto: update` 에 의존합니다. → [알려진 이슈](docs/known-issues.md)

---

## 문서

| 문서 | 내용 |
|---|---|
| [요구사항 명세](docs/requirements.md) | 사용자 요구사항 14건, 인수 기준, 추적성 매트릭스 |
| [시스템 명세](docs/architecture.md) | 구성 요소 · 데이터 모델 · Redis 사용 · 쿼리 분석 |
| [API 명세](docs/api.md) | HTTP 엔드포인트 41건 + STOMP 채널 3건 |
| [의사결정 기록](docs/decisions.md) | 구조에 영향을 준 결정 7건과 그 근거 |
| [테스트 전략](docs/testing.md) | 테스트 구성 · 인프라 · 실환경 검증 |
| [알려진 이슈](docs/known-issues.md) | 식별된 결함 12건과 조치 우선순위 |
| [작업 기록](docs/worklog-2026-08.md) | 작업 이력과 검증 데이터 |
| [인수인계](docs/handoff.md) | 새 작업자가 바로 이어받을 수 있는 문서 |

---

## 개발자

| 이름 | GitHub |
|---|---|
| 구본우 | [@BonuKoo](https://github.com/BonuKoo) |
