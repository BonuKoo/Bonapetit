# 알려진 이슈

| 항목 | 내용 |
|---|---|
| 문서 버전 | 2.1 |
| 작성 기준일 | 2026-09-01 |
| 기준 커밋 | `master` · `c10f708` |

전체 소스 정독 과정에서 식별된 결함과 개선 필요 사항입니다. 심각도는 이용자 피해 가능성과 데이터 영향 범위를 기준으로 판정했습니다.

> **이 프로젝트는 완성품이 아니라 개선 중인 시스템입니다.** 그래서 잘 된 것뿐 아니라 아직 잘못된 것도 함께 문서화합니다.
>
> 문서에 "동작한다"고 적힌 기능이 실제로는 동작하지 않는 상태가 가장 나쁩니다. 명세서의 각 요구사항에 구현 상태(충족 / 부분 / 미충족)를 표기하고, 측정 결과로 판단이 뒤집힌 이력도 지우지 않고 남기는 이유가 그것입니다.

---

## 심각도별 요약

| 심각도 | 건수 | 항목 |
|---|---|---|
| 🔴 치명 | 0 | — |
| 🟠 높음 | 2 | ISS-02, ISS-03 |
| 🟡 중간 | 6 | ISS-04, ISS-05, ISS-06, ISS-08, ISS-10, ISS-11 |
| ⚪ 낮음 | 4 | ISS-07, ISS-09, ISS-13, ISS-14 |
| ✅ 해소 | 4 | ISS-01, ISS-12, ISS-15, ISS-16 |

---

## ISS-01
**보안 · 인가 · 🔴 치명 · ✅ 2026-09-01 해소**

모임 수정·삭제·강퇴([API-19/20/21/22/23](api.md#4-모임)), 참여 해제·회원 탈퇴([API-11/12](api.md#3-프로필)), 채팅방 정보 조회([API-26](api.md#5-채팅)), 채팅 구독([WS-03](api.md#stomp-채널))에 소유자·멤버십 검증이 없었습니다.

| 지점 | 없던 검증 | 결과 |
|---|---|---|
| `PostController` 수정·삭제·강퇴·수정화면·팀원목록 | 요청자가 개설자인가 | 로그인만 하면 임의의 `teamId`로 타인의 모임을 수정·삭제하고 참여자를 강퇴 |
| `AccountProfileController.leaveTeam` | 대상이 요청자 본인인가 | 타인의 `account_id`를 실어 보내면 그 사람을 모임에서 제외 |
| `AccountProfileController.deleteAccount` | 대상이 요청자 본인인가 | 타인의 `oauth2_id`를 실어 보내면 **그 계정이 삭제됨** |
| `ChatRoomController.enterRoom` | 요청자가 그 모임의 멤버인가 | 타 모임의 `roomId`·방 이름 노출 |
| `StompHandler` SUBSCRIBE | 요청자가 그 방의 멤버인가 | `roomId`만 알면 타 모임 대화를 실시간 열람 |

**공통 원인** — "누구를"을 요청 파라미터에서 읽고 세션 주체와 대조하지 않았습니다. 화면이 자기 값을 채워 보낼 뿐이라 값을 바꿔 보내면 그대로 통했습니다.

> **회원 탈퇴(`deleteAccount`)는 원래 이 목록에 없었습니다.** `leaveTeam`을 고치다 같은 파일에서 발견했고, 같은 결함이며 피해가 더 크므로(계정 삭제) 함께 고쳤습니다.

**조치** — 인가 검사를 `TeamAccessService`(teamId 기준)와 `ChatRoomMembershipVerifier`(roomId 기준)로 모으고, 대상은 언제나 세션 주체에서 정하도록 바꿨습니다. 설계 근거는 [ADR-008](decisions.md#adr-008-인가-검사를-한곳에-모으고-필요한-두-값만-프로젝션한다)에 있습니다.

검증은 테스트 35건으로 고정했습니다(`TeamAccessServiceTest` · `TeamAccessQueryCountTest` · `ChatRoomMembershipVerifierTest` · `StompHandlerTest` · `PostControllerTest` · `AccountProfileControllerTest`). 이 과정에서 계정·모임 도메인에 처음으로 자동 검증이 생겼습니다.

**남은 것** — [ISS-13](#iss-13)(채팅방 목록·정보 노출), [ISS-14](#iss-14)(채팅 인가 쿼리 2건으로 축소 여지). 둘 다 이번 조치로 실제 피해 경로는 막혔습니다.

## ISS-02
**보안 · 권한 · 🟠 높음**

권한 문자열이 세 갈래로 불일치합니다.

| 출처 | 값 |
|---|---|
| 소셜 로그인 (`CustomOAuth2UserService`) | `ROLE_USER` / `ROLE_ADMIN` |
| 폼 회원가입 (`AccountMyBatisService.join`) | `USER_ROLE` |
| enum 정의 (`UserRole`) | `USER_ROLE`, `USER_ADMIN` |
| 인가 검사 (`NoticeController`) | `hasRole('ROLE_ADMIN')` |

추가로 `AccountProfileController.updateEmployee`가 인증 객체를 재구성하면서 권한을 `ROLE_USER`로 **하드코딩**합니다.

**영향** — 관리자가 표시 이름을 바꾸면 해당 세션에서 관리자 권한을 상실합니다. 폼 가입 계정은 유효한 권한을 갖지 못합니다.

## ISS-03
**보안 · 비밀정보 · 🟠 높음**

소스에 하드코딩된 외부 서비스 키가 공개 저장소에 노출되어 있습니다.

| 위치 | 노출 항목 |
|---|---|
| `LogoutController` | 카카오 REST API 키 |
| `LogoutService` | 네이버 `client_id`, `client_secret` |

**영향** — 제3자가 해당 키로 API를 호출할 수 있습니다.

**조치** — 각 콘솔에서 재발급하고 `${...}` 프로퍼티 주입으로 전환해야 합니다. 코드 수정과 키 재발급을 함께 수행해야 의미가 있습니다.

## ISS-04
**정합성 · 🟡 중간**

채팅방 Redis 캐시(`CHAT_ROOM` 해시)가 생성 시 1회만 기록되고 갱신·복구 경로가 없습니다.

**영향** — 참여자 변동이 캐시에 반영되지 않습니다. Redis가 초기화되면 방 목록이 비어 보이며, RDBMS에서 다시 채우는 로직이 없습니다.

**조치 방향** — 조회 시 cache-aside 패턴 적용, 또는 기동 시 warm-up.

> 이후 추가된 방 진입 캐시(`CHAT_RECENT:*`, `CHAT_AUTH:*`)는 이 문제를 갖지 않습니다. 미스 시 DB에서 통째로 채우고, 쓰기 시 write-through로 갱신하며, 탈퇴·강퇴 시 무효화합니다. **이 ISS-04는 기존 `CHAT_ROOM` 해시에만 해당합니다.**

## ISS-05
**기능 누락 · 🟡 중간**

`/profile/appliedTeam`, `/profile/chatRoom`이 뷰 이름만 반환하고 모델에 데이터를 담지 않습니다.

**영향** — [UR-14](requirements.md#ur-14--미충족) 미충족. 빈 화면이 노출됩니다.

## ISS-06
**기능 누락 · 🟡 중간**

`@EnableCaching`과 `noticeCacheManager` 빈은 설정돼 있으나, 유일한 `@Cacheable`이 `NoticeService.searchWithPage`에서 **주석 처리**되어 캐싱이 동작하지 않습니다.

**영향** — 캐시 인프라만 존재하고 효과는 없습니다. 문서 1.0의 "공지 캐싱 구현" 기술은 오기였으며 2.0에서 정정했습니다.

## ISS-07
**설정 · ⚪ 낮음**

- `ChatRoom`에 `@Table`이 없어 테이블명이 `CHATROOM`입니다. `DBConfig`가 `EntityManagerFactory`를 직접 구성해 Spring Boot의 카멜→스네이크 명명 전략이 적용되지 않습니다.
- `DBConfig`의 스캔 경로 `com.eatmate.global.domain`이 실제 패키지 `com.eatmate.domain.global`과 불일치합니다. `BaseTimeEntity`가 `@MappedSuperclass`라 상속을 통해 인식되어 동작에는 영향이 없습니다.

**영향** — 명명 규칙 불일치. 동작 영향 없음.

## ISS-08
**설정 · 🟡 중간**

`spring.` 하위에 잘못 위치해 바인딩되지 않는 키 3종입니다.

| 현재 | 올바른 키 | 미적용 결과 |
|---|---|---|
| `spring.server.port: 80` | `server.port` | 운영 포트 80이 적용된 적 없음 |
| `spring.logging.level.*` | `logging.level.*` | 로그 레벨 설정 무시 |
| `spring.session.servlet.session.timeout` | `server.servlet.session.timeout` | 세션 타임아웃 미적용 |

**주의** — 로그 레벨을 실제로 적용하면 dev 프로필이 `root: DEBUG`이므로 출력량이 급증합니다. 값을 함께 조정해야 합니다.

## ISS-09
**기능 · ⚪ 낮음**

폼 로그인 경로(`CustomAuthenticationProvider`, `FormUserDetailsService`)는 존재하나, 가입 흐름이 소셜 전용이고 시드 계정도 없어 실사용 경로가 없습니다.

**영향** — 사용되지 않는 인증 경로가 유지보수 대상으로 남습니다.

## ISS-10
**운영 · 🟡 중간**

스키마 마이그레이션 도구(Flyway·Liquibase) 없이 `ddl-auto: update`에 의존합니다. Dockerfile·CI·배포 스크립트도 없어 서버가 수동 구성 상태입니다.

**영향** — 스키마 변경 이력을 추적할 수 없습니다. 열거형 컬럼 변경 등은 `update`로 자동 반영되지 않습니다.

## ISS-11
**보안 · 설계 · 🟡 중간**

- CSRF가 비활성인 상태에서 상태 변경 작업이 `GET`으로 노출됩니다([API-37](api.md#6-공지) 공지 삭제).
- 전역 예외 처리기가 없어 내부 오류가 그대로 노출됩니다.

**영향** — 링크 프리페치나 크롤러 접근만으로 삭제가 발생할 수 있습니다. 오류 응답에 스택 정보가 포함될 수 있습니다.

## ISS-12
**성능 · 🟡 중간 · ✅ 2026-09-01 해소**

`TeamJpaService.getList`가 모임 목록을 조회한 뒤 페이지의 각 항목마다 개설자를 다시 조회했습니다.

### 21건이라고 적혀 있었지만 실제로는 42건이었습니다

문서 2.0의 "10건 페이지당 최대 21회"는 **코드를 읽어 센 숫자**였습니다. Hibernate `Statistics`로 재니 두 배였습니다.

| 출처 | 건수 | 원인 |
|---|---|---|
| JPQL | 12 | 목록 1 + `count` 1 + 개설자 조회 **10** |
| 컬렉션 로드 | 10 | `Team.getMembersCount()`가 `members.size()` — **인원수를 세려고 참여자를 전부 로드** |
| 나머지 | 20 | `AccountTeam`의 `@ManyToOne`에 `fetch` 미지정 → **EAGER** |

읽어서 셀 때 놓친 것이 뒤의 둘입니다. 인원수 세기가 컬렉션 로드라는 점과, `@ManyToOne` 기본값이 EAGER라 연관을 채우는 SELECT가 따라붙는다는 점입니다. **후자는 [ADR-008](decisions.md#adr-008-인가-검사를-한곳에-모으고-필요한-두-값만-프로젝션한다)에서 인가 쿼리가 1건인 줄 알았다가 4건으로 나왔던 것과 같은 원인**입니다.

### 조치

`TeamListRow` 프로젝션으로 **42 → 2건**(목록 1 + `count` 1)입니다. 엔티티를 만들지 않으면 EAGER 연관도 컬렉션도 따라오지 않습니다. 인원수는 `size()`가 상관 서브쿼리로 번역되어 같은 쿼리 안에서 계산됩니다.

`TeamListQueryCountTest`가 2건을 고정하고, `TeamListContentTest`가 결과 내용(개설자 닉네임·인원수·검색·날짜 형식)을 검증합니다.

곁들여 **전체 건수와 항목 수의 불일치**를 고쳤습니다. 예전에는 개설자 없는 모임을 목록에서 건너뛰면서도 전체 건수에는 그대로 두어 페이지에 빈자리가 생겼습니다. `count` 쿼리에도 같은 조인을 걸었습니다.

> 쿼리 **개수**는 DB와 무관하게 측정됩니다. 다만 상관 서브쿼리의 **실행 계획**은 MySQL에서 확인하지 않았습니다. 이 프로젝트는 H2와 MySQL의 계획이 갈려 결론이 뒤집힌 적이 있습니다.

## ISS-15
**정합성 · 🔴 치명 · ✅ 2026-09-01 해소**

대화에 한 번이라도 참여한 사용자는 **회원 탈퇴([API-11](api.md#3-프로필))가 실패**했습니다.

```
ConstraintViolationException: Referential integrity constraint violation
```

`account`를 참조하는 테이블은 셋입니다. `AccountMyBatisService.deleteUserByOauth2Id`가 그중 둘만 정리했습니다.

| 참조 테이블 | 처리 |
|---|---|
| `notice.account_id` | 삭제 ✅ |
| `account_team.account_id` | 삭제 ✅ |
| `chat_message.account_id` | **누락** ❌ |

**언제부터** — 메시지 영속화(PR [#70](https://github.com/BonuKoo/Bonapetit/pull/70))로 `chat_message`가 생기면서입니다. 그전에는 참조 테이블이 둘뿐이라 맞는 코드였고, 새 테이블이 추가될 때 이 경로가 따라오지 않았습니다.

**왜 안 걸렸나** — [UR-03](requirements.md#ur-03--충족)이 검증 없이 "충족"으로 적혀 있었고, 계정 도메인에 자동 테스트가 없었습니다. 성능 측정 중 `chat_message.account_id`의 FK를 살펴보다 발견했습니다.

**조치** — 삭제에 앞서 발신자 연결만 끊습니다(`ChatMessageDao.detachSenderByAccountId`). 대화를 지우지 않는 것은 설계 의도입니다. `ChatMessage.sender`가 nullable이고 표시 이름을 `sender_name` 스냅샷으로 따로 들고 있는 이유가 이것입니다 — 한 사람이 나갔다고 남은 참여자들의 대화 맥락까지 사라지면 안 됩니다.

`AccountDeletionTest`가 서비스를 직접 호출해 고정합니다. 결함이 SQL이 아니라 **서비스가 한 단계를 빠뜨린 것**이었으므로, SQL만 검증하면 같은 실수를 다시 놓칩니다. 수정을 되돌리면 2건 모두 실패하는 것을 확인했습니다.

## ISS-16
**정합성 · 🔴 치명 · ✅ 2026-09-01 해소**

대화가 오간 모임은 **삭제([API-21](api.md#4-모임))가 실패**했습니다. [ISS-15](#iss-15)와 같은 계열이며, 같은 조사에서 이어서 찾았습니다.

```
ConstraintViolationException:
  CHAT_MESSAGE FOREIGN KEY(ROOM_ID) REFERENCES CHAT_ROOM(ROOM_ID)
  SQL: delete from chat_room where room_id=?
```

`Team`은 `cascade = ALL`로 `AccountTeam`과 `ChatRoom`을 함께 지웁니다. 그런데 `ChatRoom`에는 메시지 컬렉션이 없어 **cascade가 `chat_message`까지 닿지 않습니다.**

**ISS-15와 처방이 다릅니다.**

| | ISS-15 (회원 탈퇴) | ISS-16 (모임 삭제) |
|---|---|---|
| 끊어야 할 FK | `chat_message.account_id` | `chat_message.room_id` |
| 컬럼 | nullable | **`nullable = false`** |
| 처방 | NULL로 연결만 끊고 대화 보존 | 방과 함께 삭제 |

탈퇴는 "남은 참여자들의 대화를 지키는" 문제지만, 모임 삭제는 그 대화의 주인인 모임 자체가 사라지므로 함께 지우는 것이 맞습니다.

**조치**

- `ChatMessageRepository.deleteByRoomId` — DELETE 한 문장. `ChatRoom`에 `@OneToMany(cascade = ALL)`을 다는 방법은 쓰지 않았습니다. 그러면 방 삭제 때 메시지를 전부 영속성 컨텍스트에 올려 한 건씩 지워, 대화가 많은 방일수록 비용이 선형으로 커집니다
- 삭제 로직을 `PostController`에서 `PostJpaService.deleteTeam`으로 옮기고 `@Transactional`을 걸었습니다. 단계가 여럿이 되면서, 중간에 실패해 메시지만 지워지고 모임이 남는 상태를 막아야 합니다
- **캐시 무효화** — 모임 삭제는 참여자 전원에게서 권한을 빼앗는 작업이라 강퇴·탈퇴와 같은 이유로 즉시 무효화합니다([ADR-005](decisions.md#adr-005-방-진입-경로를-캐싱한다-첫-페이지--멤버십)). 그러지 않으면 멤버십 캐시가 살아 있는 동안 인가를 통과하고 방 조회를 건너뛰므로, **사라진 모임의 대화가 최대 5분간 계속 읽힙니다.** `CHAT_ROOM` 해시는 TTL이 없어 지우지 않으면 사라진 방이 목록에 영원히 남습니다

> `CHAT_ROOM` 해시를 이 경로에서 지우게 되어, [ISS-04](#iss-04)의 "갱신·복구 경로가 없다" 중 **삭제 경로만** 생겼습니다. 조회 시 cache-aside나 기동 시 warm-up은 여전히 없습니다.

## ISS-13
**보안 · 정보 노출 · ⚪ 낮음**

`ChatRoomController`의 방 목록·방 정보 조회가 멤버십을 확인하지 않습니다.

| 위치 | 노출 |
|---|---|
| `GET /chat/rooms`([API-28](api.md#5-채팅)) | Redis에 있는 **모든 채팅방**의 `roomId`·방 이름·접속자 수 |
| `GET /chat/room/{roomId}`([API-27](api.md#5-채팅)) | 임의 방의 이름·접속자 수 |

**영향** — 방 이름과 접속자 수 수준의 정보가 새어 나갑니다. [ISS-01](#iss-01) 조치로 `roomId`를 알아도 구독과 내역 조회는 막히므로 **대화 자체는 볼 수 없습니다.** 그래서 낮음입니다.

**조치 방향** — "내가 속한 방 목록" 조회로 바꿔야 합니다. [ISS-05](#iss-05)의 `/profile/chatRoom` 화면과 같은 쿼리가 필요하므로 함께 처리하는 것이 낫습니다.

## ISS-14
**성능 · ⚪ 낮음**

채팅 멤버십 검증(`ChatRoomMembershipVerifier`)이 캐시 미스 시 쿼리 3건을 씁니다(방 조회 · 계정 조회 · 멤버십 조회).

[ISS-01](#iss-01) 조치로 도입한 `AccountTeamRepository.findMembership`을 쓰면 계정 조회가 사라져 2건이 됩니다.

**보류한 이유** — 보안 수정에 성능 변경을 섞으면 검토가 어려워집니다. 또 이 숫자는 [ADR-005](decisions.md#adr-005-방-진입-경로를-캐싱한다-첫-페이지--멤버십)의 근거이자 `ChatHistoryQueryCountTest`가 4/1/0으로 고정한 값이라, 바꾸려면 문서와 테스트를 함께 갱신해야 합니다.

**영향** — 캐시 미스일 때만이고 TTL 5분이라 실사용 빈도가 낮습니다.

---

---

## 이슈는 아니지만 알아둘 것

### 개발(H2)과 운영(MySQL)의 실행 계획이 다릅니다

같은 쿼리·같은 인덱스인데 두 DB의 실행 계획이 갈립니다. 실측 결과입니다.

| 쿼리 | H2 (방 234건) | MySQL (방 5,000건 / 총 11만 건) |
|---|---|---|
| 채팅 내역 첫 페이지 | 방 전체 읽음 — `scanCount: 235` | **실제 51행** — `Backward index scan` |

H2는 `ORDER BY id DESC LIMIT`을 인덱스 역방향 스캔으로 처리하지 못해 방 전체를 읽고 정렬합니다. MySQL은 `LIMIT`에서 조기 종료합니다.

**성능 판단을 H2에서만 하면 잘못된 결론에 도달합니다.** 실제로 이 프로젝트에서 한 번 겪었고, MySQL 재측정으로 정정했습니다(→ [작업 기록 6.3](worklog-2026-08.md#63-mysql-재측정--결론이-뒤집힌-지점)).

부수적으로, Hibernate가 FK용으로 만드는 `room_id` 단일 인덱스는 **H2에서만** 생성됩니다. MySQL은 복합 인덱스의 접두사가 FK를 커버하므로 중복을 만들지 않습니다.

---

## 조치 우선순위

| 순위 | 대상 | 근거 |
|---|---|---|
| ~~1~~ | ~~**ISS-01** 인가 검증 추가~~ | ✅ 2026-09-01 해소 |
| ~~1~~ | ~~**ISS-15** 탈퇴 FK 누락~~ | ✅ 2026-09-01 해소 |
| ~~1~~ | ~~**ISS-16** 모임 삭제 FK 누락~~ | ✅ 2026-09-01 해소 |
| ~~5~~ | ~~**ISS-12** 모임 목록 N+1~~ | ✅ 2026-09-01 해소 (42 → 2건) |
| 1 | **ISS-03** 키 재발급 및 분리 | 노출이 진행 중인 상태. 코드 수정과 콘솔 재발급을 함께 수행해야 함 |
| 2 | **ISS-02** 권한 체계 통일 | 관리자 기능이 예측 불가능하게 동작. 문자열 상수 통일 + 인증 객체 재구성 로직 수정. `StompHandler`가 인증 객체 타입이 바뀌는 것에 방어 코드를 두고 있는데, ISS-02를 고치면 그 방어가 필요 없어집니다 |
| 3 | **ISS-08** 설정 키 위치 교정 | 수정 비용이 낮고 운영 포트·세션 타임아웃이 정상화됨. 로그 레벨 적용 시 출력량 급증에 유의 |
| 4 | **ISS-05 + ISS-13** | 프로필의 빈 화면과 채팅방 목록 노출이 같은 쿼리를 필요로 함. 함께 처리 |
| 5 | **ISS-06** | 공지 캐싱. 부하 측정 후 착수 판단 |

---

관련 문서: [요구사항 명세](requirements.md) · [시스템 명세](architecture.md) · [API 명세](api.md) · [의사결정 기록](decisions.md) · [테스트 전략](testing.md)
