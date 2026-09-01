package com.eatmate.loadtest;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chat.ChatMessageRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 채팅 발송 경로 부하 하니스.
 *
 * <h3>왜 필요한가</h3>
 * {@code application-prod.yml} 의 Tomcat 스레드 50 · Hikari 20 은 <b>잠정값</b>이고
 * "k6 재측정 후 조정" 주석이 달려 있다. 지금까지 이 프로젝트에는 부하 수치가 <b>하나도
 * 없다.</b> 남은 성능 후보(발송 경로의 계정 SELECT 제거, 인가 쿼리 3 -&gt; 2 등)를 손대야
 * 할지도 근거가 없다. 그 근거를 만드는 것이 목적이다.
 *
 * <h3>기본으로는 돌지 않는다</h3>
 * {@code -Dloadtest=true} 가 있어야 실행된다. Redis 가 필요하고 수십 초가 걸려서
 * 일반 {@code ./gradlew test} 에 섞이면 안 된다.
 *
 * <pre>
 * docker run -d --name eatmate-redis -p 6379:6379 redis
 * ./gradlew test --tests "*ChatSendLoadTest" -Dloadtest=true -Dloadtest.users=20
 * </pre>
 *
 * <h3>답할 수 있는 것과 없는 것</h3>
 * <ul>
 *   <li>답함 - 동시 접속이 늘 때 지연이 어디서 꺾이는지, 개선 전후 비교, 유실 여부</li>
 *   <li><b>못 함</b> - 절대 처리량. 생성기와 서버가 <b>같은 JVM</b>이라 CPU 를 나눠 쓴다</li>
 *   <li><b>못 함</b> - 운영 DB 기준 수치. 기본은 인메모리 H2 다. 이 프로젝트는 H2 와
 *       MySQL 의 실행 계획이 갈려 결론이 뒤집힌 적이 있다</li>
 * </ul>
 * 지금은 수치가 아예 없는 상태라, 상대 비교만으로도 출발점이 된다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(LoadTestSecurityConfig.class)
@EnabledIfSystemProperty(named = "loadtest", matches = "true",
        disabledReason = "Redis 가 필요하고 오래 걸린다. -Dloadtest=true 로 실행한다")
class ChatSendLoadTest {

    private static final int USERS = Integer.getInteger("loadtest.users", 10);
    private static final int MESSAGES_PER_USER = Integer.getInteger("loadtest.messages", 20);

    /** 사용자 한 명의 발송 간격. 사람이 치는 속도를 흉내낸다. */
    private static final long SEND_INTERVAL_MILLIS = Long.getLong("loadtest.intervalMillis", 50L);

    private static final String ROOM_ID = "loadtest-room";

    @LocalServerPort private int port;

    @Autowired private AccountRepository accountRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private AccountTeamRepository accountTeamRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private final RestTemplate rest = new RestTemplate();

    @BeforeEach
    void seed() {
        if (chatRoomRepository.findById(ROOM_ID).isPresent()) {
            return;
        }
        Team team = teamRepository.save(Team.builder().teamName("부하 테스트 모임").build());
        ChatRoom room = ChatRoom.builder().roomId(ROOM_ID).roomName("부하 테스트 방").team(team).build();
        room.setTeam(team);
        chatRoomRepository.save(room);

        for (int i = 0; i < USERS; i++) {
            Account account = accountRepository.save(Account.builder()
                    .email("load" + i + "@eatmate.com")
                    .nickname("부하" + i)
                    .password("x").build());
            setOauth2Id(account, oauth2Id(i));
            accountRepository.save(account);
            // 구독에는 멤버십이 필요하다(ISS-01 조치).
            accountTeamRepository.save(AccountTeam.builder()
                    .account(account).team(team).isLeader(i == 0).build());
        }
    }

    private String oauth2Id(int index) {
        return "loadtest-oauth-" + index;
    }

    private void setOauth2Id(Account account, String value) {
        try {
            var f = Account.class.getDeclaredField("oauth2id");
            f.setAccessible(true);
            f.set(account, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("동시 사용자가 메시지를 보낼 때의 처리량과 지연을 잰다")
    void measureChatSendThroughput() throws Exception {
        LoadStats stats = new LoadStats();
        // 보낸 시각을 메시지 본문으로 되찾는다. 서버가 그대로 돌려주므로 왕복을 잴 수 있다.
        Map<String, Long> sentAtNanos = new ConcurrentHashMap<>();

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.initialize();

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        CountDownLatch allDone = new CountDownLatch(USERS);
        CountDownLatch received = new CountDownLatch(USERS * MESSAGES_PER_USER);

        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < USERS; i++) {
            final int userIndex = i;
            pool.submit(() -> {
                try {
                    runVirtualUser(userIndex, scheduler, stats, sentAtNanos, received);
                } catch (Exception e) {
                    stats.recordFailure();
                    System.out.println("### 사용자 " + userIndex + " 실패: " + e);
                } finally {
                    allDone.countDown();
                }
            });
        }

        allDone.await(3, TimeUnit.MINUTES);
        received.await(30, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startedAt;

        pool.shutdownNow();
        scheduler.shutdown();

        System.out.println(stats.report(
                "채팅 발송 · 사용자 " + USERS + " · 인당 " + MESSAGES_PER_USER + "건", elapsed));

        int persisted = chatMessageRepository
                .findLatestByRoomId(ROOM_ID, PageRequest.of(0, Integer.MAX_VALUE)).size();
        System.out.println("### DB 에 저장된 TALK = " + persisted);

        // 성능 수치에는 단정을 걸지 않는다. 환경마다 다르고, 단정을 걸면 그 숫자가
        // 목표처럼 굳어 버린다. 다만 유실은 명백한 결함이라 여기서 잡는다.
        assertThat(stats.failedCount()).as("연결·발송 자체가 실패한 사용자").isZero();
        assertThat(persisted).as("보낸 메시지는 모두 저장되어야 한다")
                .isEqualTo(USERS * MESSAGES_PER_USER);
    }

    private void runVirtualUser(int userIndex,
                                ThreadPoolTaskScheduler scheduler,
                                LoadStats stats,
                                Map<String, Long> sentAtNanos,
                                CountDownLatch received) throws Exception {
        String oauth2Id = oauth2Id(userIndex);
        String jwt = jwtTokenProvider.generateToken(oauth2Id, "부하" + userIndex);
        String cookie = login(oauth2Id);

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        client.setTaskScheduler(scheduler);

        WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
        handshake.add(HttpHeaders.COOKIE, cookie);

        StompHeaders connect = new StompHeaders();
        connect.add("token", jwt);

        // SockJS 를 쓰지만 raw WebSocket 경로가 열려 있어 그쪽으로 붙는다.
        StompSession session = client
                .connectAsync("ws://localhost:" + port + "/ws-stomp/websocket",
                        handshake, connect, new StompSessionHandlerAdapter() { })
                .get(20, TimeUnit.SECONDS);

        session.subscribe("/sub/chat/room/" + ROOM_ID, new StompFrameHandler() {
            @Override
            public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                Object text = ((Map<String, Object>) payload).get("message");
                Long sentAt = (text == null) ? null : sentAtNanos.remove(text.toString());
                if (sentAt != null) {
                    stats.recordLatency(System.nanoTime() - sentAt);
                    received.countDown();
                }
            }
        });

        StompHeaders send = new StompHeaders();
        send.setDestination("/pub/chat/message");
        send.add("token", jwt);

        for (int m = 0; m < MESSAGES_PER_USER; m++) {
            String text = "u" + userIndex + "-m" + m;
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", ChatMessage.MessageType.TALK.name());
            payload.put("roomId", ROOM_ID);
            payload.put("message", text);

            sentAtNanos.put(text, System.nanoTime());
            stats.recordSent();
            session.send(send, payload);

            Thread.sleep(SEND_INTERVAL_MILLIS);
        }

        // 마지막 메시지가 돌아올 여유를 준다.
        Thread.sleep(2_000);
        session.disconnect();
    }

    /**
     * 테스트 전용 로그인으로 세션 쿠키를 얻는다. 그 밖의 경로는 운영 보안 설정을 그대로 지난다.
     *
     * <p>쿠키 이름을 짐작하지 않고 응답의 {@code Set-Cookie} 를 그대로 되돌려 보낸다.
     * 이 앱은 클래스패스에 spring-session-data-redis 가 있어 세션이 Redis 에 저장되고
     * 쿠키가 {@code JSESSIONID} 가 아니라 <b>{@code SESSION}</b>(값은 base64)이다.
     * 이름을 박아 두면 세션 저장소를 바꾸는 순간 조용히 깨진다.
     */
    private String login(String oauth2Id) {
        String url = "http://localhost:" + port + "/test-only/login?oauth2Id=" + oauth2Id;
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies == null || setCookies.isEmpty()) {
            throw new IllegalStateException("세션 쿠키를 받지 못했다: " + url);
        }
        // "이름=값; Path=/; HttpOnly" 에서 이름=값만 남긴다.
        return setCookies.stream()
                .map(header -> header.split(";", 2)[0])
                .collect(Collectors.joining("; "));
    }
}
