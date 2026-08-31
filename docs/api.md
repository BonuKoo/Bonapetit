# API 명세서

| 항목 | 내용 |
|---|---|
| 문서 버전 | 2.0 |
| 작성 기준일 | 2026-08-31 |
| 기준 커밋 | `master` · `7ebe9b0` |

HTTP 엔드포인트와 STOMP 채널. 응답 유형이 **뷰**인 것은 Thymeleaf 템플릿을, **JSON**인 것은 본문을 반환합니다.

---

## 1. 공통 규약

| 항목 | 규약 |
|---|---|
| 기본 URL | 개발 `http://localhost:8080` · 운영 `https://api.bonappetit.p-e.kr` |
| 인증 | 세션 쿠키(`JSESSIONID`). 미인증 요청은 `302 → /login` |
| WebSocket 인증 | STOMP CONNECT 헤더 `token`에 JWT |
| 인코딩 | UTF-8 |
| CSRF | **비활성**. 상태 변경 요청에 토큰 불필요 → [ISS-11](known-issues.md#iss-11) |
| 오류 응답 | 전역 예외 처리기 부재. 도메인 예외는 대부분 `500`으로 노출됨. 채팅 내역 조회만 `403`/`404`를 명시적으로 반환 |

**접근 권한 표기** — 🟢 공개(인증 불요) · 🔵 인증(로그인 필요) · 🟣 멤버(해당 모임 참여자) · 🔴 관리자(`ROLE_ADMIN`)

---

## 2. 인증 · 계정

| ID | 메서드 | 경로 | 요청 | 응답 | 권한 |
|---|---|---|---|---|---|
| API-01 | GET | `/login` | — | 뷰 | 🟢 |
| API-02 | GET | `/join` | — | 뷰 | 🟢 |
| API-03 | POST | `/join` | `email`, `nick_name`, `password` | 리다이렉트 | 🟢 |
| API-04 | GET | `/login/oauth2/code/{provider}` | OAuth 콜백 (`code`, `state`) | 리다이렉트 → `/` | 🟢 |
| API-05 | GET | `/logout` | — | 리다이렉트 | 🔵 |
| API-06 | GET | `/{kakao\|naver\|google}/logout` | — | 리다이렉트 · 제공자 연동 해제 | 🟢 |
| API-07 | GET | `/get-provider` | — | JSON · 현재 세션의 provider | 🔵 |
| API-08 | GET | `/user/info` | — | JSON `{ oauthId, nickname, token }` | 🔵 |

> **API-08**이 반환하는 `token`은 WebSocket 연결 및 메시지 발행에 사용하는 JWT입니다(HS256, 유효기간 1시간). 클레임은 `jti`=oauth2Id, `nickname`입니다.

## 3. 프로필

| ID | 메서드 | 경로 | 요청 | 응답 | 권한 |
|---|---|---|---|---|---|
| API-09 | GET | `/profile/list` | — | 뷰 · 프로필, 소속/개설/참여 모임 | 🔵 |
| API-10 | POST | `/profile/detail` | `oauth2_id`, `nick_name`, `password`(선택) | 리다이렉트 · 세션 갱신 | 🔵 |
| API-11 | POST | `/profile/delete` | `oauth2_id`, `account_id` | 리다이렉트 · 계정+참여+공지 삭제, 연동 해제 | 🔵 |
| API-12 | POST | `/profile/leaveTeam` | `account_id`, `team_id` | 리다이렉트 | 🔵 |
| API-13 | GET | `/profile/appliedTeam` | — | 뷰 (**데이터 미제공**) | 🔵 |
| API-14 | GET | `/profile/chatRoom` | — | 뷰 (**데이터 미제공**) | 🔵 |

> ⚠️ **API-10 부작용** — 표시 이름 수정 시 인증 객체를 재구성하면서 권한을 `ROLE_USER`로 **하드코딩**합니다. 따라서 관리자가 표시 이름을 바꾸면 그 세션에서 관리자 권한을 잃습니다. → [ISS-02](known-issues.md#iss-02)
>
> ⚠️ **API-11·12 인가** — `account_id`를 요청 파라미터로 받고 세션 주체와 대조하지 않습니다. 타인의 식별자를 넣으면 타인의 참여를 해제할 수 있습니다. → [ISS-01](known-issues.md#iss-01)

## 4. 모임

| ID | 메서드 | 경로 | 요청 | 응답 | 권한 |
|---|---|---|---|---|---|
| API-15 | GET | `/post/create` | — | 뷰 | 🔵 |
| API-16 | POST | `/post/createPost` | `teamName`, `description`, `mapId`, `placeName`, `addressName`, `roadAddressName`, `phone`, `placeUrl`, `x`, `y` | 리다이렉트 → `/post/list`<br>모임 + 채팅방 + 개설자 참여 생성 | 🔵 |
| API-17 | GET | `/post/list` | `page`=1, `keyword`="" | 뷰 · 10건 단위. 이름·장소·주소 검색 | 🟢 |
| API-18 | GET | `/post/detail/{teamId}` | — | 뷰 | 🟢 |
| API-19 | GET | `/post/update/{teamId}` | — | 뷰 | 🔵 |
| API-20 | POST | `/post/updateTeam` | `teamId`, `teamName`, `description`, 장소 정보 | 리다이렉트 | 🔵 |
| API-21 | POST | `/post/deleteTeam` | `teamId` | 리다이렉트 → `/` | 🔵 |
| API-22 | GET | `/post/members/{teamId}` | — | 뷰 · 참여자 목록 | 🔵 |
| API-23 | POST | `/post/kickMember` | `account_id`, `team_id` | 리다이렉트 | 🔵 |
| API-24 | POST | `/team/join/{teamId}` | — | `200 OK` (본문 없음) | 🔵 |

> ⚠️ **API-20·21·23은 개설자 여부를 검증하지 않습니다.** 로그인한 사용자면 임의의 `teamId`로 타인의 모임을 수정·삭제하거나 참여자를 강퇴할 수 있습니다. → [ISS-01](known-issues.md#iss-01)

## 5. 채팅

| ID | 메서드 | 경로 | 요청 | 응답 | 권한 |
|---|---|---|---|---|---|
| API-25 | GET | `/chat/room/{roomId}/messages` | `before` 커서(선택), `size` 기본 50 · 최대 100 | JSON `ChatHistoryResponse` | 🟣 |
| API-26 | POST | `/chat/enter/{teamId}` | — | JSON `{ roomId, roomName }` | 🔵 |
| API-27 | GET | `/chat/room/{roomId}` | — | JSON `ChatRoomDTO` (Redis 캐시) | 🔵 |
| API-28 | GET | `/chat/rooms` | — | JSON `ChatRoomDTO[]` · **전체 방** | 🔵 |
| API-29 | GET | `/chat/room` | — | 뷰 · 방 목록 | 🔵 |
| API-30 | GET | `/chat/room/enter/{roomId}` | — | 뷰 · 채팅 화면 | 🔵 |

### API-25 상세 — 대화 내역 조회

```
GET /chat/room/{roomId}/messages?before={id}&size=50
```

**요청 파라미터**

| 이름 | 위치 | 필수 | 설명 |
|---|---|---|---|
| `roomId` | path | 필수 | 채팅방 UUID |
| `before` | query | 선택 | 커서. 이 id **미만**의 메시지를 조회. 미지정 시 최신부터 |
| `size` | query | 선택 | 1–100 범위로 보정. 기본 50 |

**응답 `200 OK`**

```json
{
  "messages": [
    {
      "id": 101,
      "sender": "홍길동",
      "message": "7시에 봬요",
      "type": "TALK",
      "createdAt": "2026-08-31T16:03:24.348389"
    }
  ],
  "nextCursor": 101,
  "hasMore": true
}
```

| 필드 | 설명 |
|---|---|
| `messages` | **오래된 것 → 최신 순**. 화면 렌더링 순서에 맞춰 서버에서 뒤집어 반환 |
| `messages[].sender` | 발신 시점 표시 이름 스냅샷 |
| `nextCursor` | 다음 요청의 `before`. 결과 없으면 `null` |
| `hasMore` | `size+1` 조회로 판정. 전체 건수는 반환하지 않음 |

**오류 응답**

| 코드 | 조건 |
|---|---|
| `403 Forbidden` | 해당 모임의 참여자가 아님 |
| `404 Not Found` | 존재하지 않는 채팅방 |
| `302 → /login` | 미인증 |

**페이징 규칙**
- 커서 비교는 배타적(`<`). `<=`로 두면 페이지마다 1건씩 중복됩니다
- 정렬 키는 `id`. `createdAt`은 동일 밀리초 충돌 가능성이 있어 부적합합니다
- 전체 건수는 반환하지 않습니다(`count` 쿼리 미실행)

**캐시 동작** (응답 형식에는 영향 없음)

| 요청 | 경로 |
|---|---|
| `before` 없음 (방 진입) | Redis `CHAT_RECENT:{roomId}` 우선. 미스면 DB에서 101건을 읽어 캐시를 채운 뒤 응답 |
| `before` 있음 (스크롤) | 항상 DB |
| 인가 검증 | Redis `CHAT_AUTH:{roomId}:{oauth2Id}` 우선. 미스면 DB 3쿼리 후 캐시 기록 |

Redis 장애 시 모든 경로가 DB로 폴백하며 응답은 동일합니다. 자세한 설계는 [시스템 명세](architecture.md#방-진입-캐시) 참조.

### STOMP 채널

| ID | 구분 | 경로 | 페이로드 | 비고 |
|---|---|---|---|---|
| WS-01 | ENDPOINT | `/ws-stomp` | — | SockJS. raw WebSocket은 `/ws-stomp/websocket`. CONNECT 시 `token` 검증 |
| WS-02 | SEND | `/pub/chat/message` | `{ type, roomId, message }`<br>헤더 `token` | 발신자는 서버가 JWT에서 확정(클라이언트 값 불신). `TALK`만 영속화 |
| WS-03 | SUBSCRIBE | `/sub/chat/room/{roomId}` | `{ id, type, roomId, sender, message, userCount }` | 구독 시 접속자 +1 및 입장 알림 발행. `id`는 `TALK`에만 존재 |

`type`은 `ENTER` · `QUIT` · `TALK`입니다. `ENTER`/`QUIT`은 서버가 발신자를 `[알림]`으로 치환하고 문구를 생성하며, 저장하지 않으므로 `id`가 `null`입니다.

> ⚠️ **WS-03 인가 없음** — 구독 시 해당 방의 참여자인지 검증하지 않습니다. `roomId`를 알면 타 모임의 실시간 대화를 구독할 수 있습니다. → [ISS-01](known-issues.md#iss-01)

## 6. 공지

| ID | 메서드 | 경로 | 요청 | 응답 | 권한 |
|---|---|---|---|---|---|
| API-31 | GET | `/notice` | `keyword`, `page`=0, `size`=10 | 뷰 · 제목 부분 일치 검색 | 🟢 |
| API-32 | GET | `/notice/detail/{noticeId}` | — | 뷰 | 🟢 |
| API-33 | GET | `/notice/create` | — | 뷰 | 🔴 |
| API-34 | POST | `/notice/create` | `title`, `content` | 리다이렉트 | 🔴 |
| API-35 | GET | `/notice/update/{id}` | — | 뷰 | 🔴 |
| API-36 | POST | `/notice/{id}` | `title`, `content` | 리다이렉트 | 🔴 |
| API-37 | GET | `/notice/delete/{id}` | — | 리다이렉트 | 🔴 |

> ⚠️ **API-37이 GET입니다.** 상태를 변경하는 작업은 `POST` 또는 `DELETE`여야 합니다. 현재 형태는 링크 프리페치나 크롤러 접근만으로도 삭제가 발생할 수 있습니다. → [ISS-11](known-issues.md#iss-11)

## 7. 기타

| ID | 메서드 | 경로 | 용도 | 권한 |
|---|---|---|---|---|
| API-38 | GET | `/` | 홈 | 🟢 |
| API-39 | GET | `/health` | 헬스 체크 | 🔵 |
| API-40 | GET | `/actuator/**` | health · info · prometheus | 🟢 |
| API-41 | GET | `/noticecompare/inefficient1–5`, `/optimized` | 공지 조회 성능 비교 실험용. **서비스 기능 아님** | 🟢 |

> **API-41**은 학습·측정 목적의 엔드포인트이며 인증 없이 열려 있습니다. 운영 배포 대상에서 제외하거나 프로필로 분리하는 것이 바람직합니다.

---

관련 문서: [요구사항 명세](requirements.md) · [시스템 명세](architecture.md) · [의사결정 기록](decisions.md) · [알려진 이슈](known-issues.md)
