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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 팬아웃이 비용인지 가르는 측정.
 *
 * <h3>왜 필요한가</h3>
 * 첫 부하 측정에서 동시 사용자 10 -&gt; 20 에 p50 이 256ms -&gt; 1,707ms 로 뛰었다.
 * 원인 후보가 둘이었다.
 * <ul>
 *   <li><b>팬아웃</b> — 한 방에 N명이면 메시지 1건이 N명에게 나간다. 보낸 건수의 N배</li>
 *   <li><b>클라이언트 비용</b> — 생성기가 서버와 같은 JVM 이라 프레임 파싱이 CPU 를 나눠 쓴다</li>
 * </ul>
 * 둘은 <b>전달 건수</b>가 같이 움직여서 첫 측정으로는 갈리지 않았다.
 * 보낸 건수를 고정한 채 전달 건수만 바꾸면 갈린다.
 *
 * <h3>시나리오</h3>
 * 셋 다 <b>보낸 건수가 같다.</b> 전달 건수만 다르다.
 * <pre>
 *   한 방 N명    보냄 S건 -> 전달 S*N건   (지금 구조)
 *   N방 1명씩    보냄 S건 -> 전달 S건     (팬아웃 없음)
 *   구독 없음    보냄 S건 -> 전달 0건     (순수 수신 처리)
 * </pre>
 *
 * <h3>공통 지표</h3>
 * 세 시나리오를 한 자로 재려면 지연은 쓸 수 없다(구독 없는 쪽은 왕복이 없다).
 * <b>첫 발송부터 DB 에 전부 저장될 때까지</b>의 시간을 잰다. 서버가 실제로 일을
 * 끝낸 시점이라 셋을 같은 기준으로 비교할 수 있다.
 *
 * <p>같은 JVM 에서 연달아 돌린다. 워밍업 상태가 같아야 비교가 성립한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(LoadTestSecurityConfig.class)
@EnabledIfSystemProperty(named = "loadtest", matches = "true",
        disabledReason = "Redis 가 필요하고 오래 걸린다. -Dloadtest=true 로 실행한다")
class ChatFanoutLoadTest {

    private static final int USERS = Integer.getInteger("loadtest.users", 20);
    private static final int MESSAGES_PER_USER = Integer.getInteger("loadtest.messages", 30);
    private static final long SEND_INTERVAL_MILLIS = Long.getLong("loadtest.intervalMillis", 50L);

    @LocalServerPort private int port;

    @Autowired private AccountRepository accountRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private AccountTeamRepository accountTeamRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private final RestTemplate rest = new RestTemplate();
    private ThreadPoolTaskScheduler scheduler;

    /** 한 시나리오의 결과. */
    private record Result(String label, int sends, long deliveries,
                          long ingestMillis, Double p50Millis, Double p95Millis) {

        double ingestPerSecond() {
            return ingestMillis > 0 ? sends * 1000.0 / ingestMillis : Double.NaN;
        }
    }

    @Test
    @DisplayName("보낸 건수를 고정하고 전달 건수만 바꿔 팬아웃 비용을 가른다")
    void compareFanoutCost() throws Exception {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.initialize();

        // 워밍업. JIT 와 커넥션 준비가 첫 시나리오에만 얹히면 비교가 기운다.
        runScenario("워밍업", "warm", 1, true, 5);

        List<Result> results = new ArrayList<>();
        results.add(runScenario("한 방 " + USERS + "명", "one", 1, true, MESSAGES_PER_USER));
        results.add(runScenario(USERS + "방 1명씩", "many", USERS, true, MESSAGES_PER_USER));
        results.add(runScenario("구독 없음", "noSub", USERS, false, MESSAGES_PER_USER));

        scheduler.shutdown();

        System.out.println();
        System.out.println("### ================= 팬아웃 비교 =================");
        System.out.println("### 사용자 " + USERS + " · 인당 " + MESSAGES_PER_USER
                + "건 · 발송 간격 " + SEND_INTERVAL_MILLIS + "ms");
        System.out.println("### 시나리오        보냄   전달    저장완료   처리량      p50      p95");
        for (Result r : results) {
            System.out.println("### " + pad(r.label(), 16)
                    + pad(String.valueOf(r.sends()), 6)
                    + pad(String.valueOf(r.deliveries()), 7)
                    + pad(r.ingestMillis() + "ms", 10)
                    + pad(String.format("%.1f/s", r.ingestPerSecond()), 11)
                    + pad(fmt(r.p50Millis()), 9)
                    + fmt(r.p95Millis()));
        }
        System.out.println("### ==============================================");

        Result oneRoom = results.get(0);
        Result manyRooms = results.get(1);
        System.out.println("### 전달 건수 비율  = " + oneRoom.deliveries() + " : " + manyRooms.deliveries());
        System.out.println("### 저장완료 시간 비 = "
                + String.format("%.2f배", (double) oneRoom.ingestMillis() / manyRooms.ingestMillis()));

        // 성능 수치에는 단정을 걸지 않는다. 다만 세 시나리오 모두 보낸 만큼 저장되어야 한다.
        for (Result r : results) {
            assertThat(r.ingestMillis()).as(r.label() + " 가 시간 안에 전부 저장되지 않았다")
                    .isPositive();
        }
    }

    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String fmt(Double v) {
        return (v == null || v.isNaN()) ? "-        " : String.format("%.0fms     ", v);
    }

    /**
     * @param roomCount 1이면 모두 한 방, USERS 면 각자 자기 방
     * @param subscribe 구독 여부. false 면 전달이 0건이라 순수 수신 처리 비용만 남는다
     */
    private Result runScenario(String label, String prefix, int roomCount,
                               boolean subscribe, int messagesPerUser) throws Exception {
        List<String> roomIds = seed(prefix, roomCount);

        int expectedSends = USERS * messagesPerUser;
        long baseline = chatMessageRepository.count();

        Map<String, Long> sentAtNanos = new ConcurrentHashMap<>();
        List<Long> latenciesMicros = java.util.Collections.synchronizedList(new ArrayList<>());
        AtomicLong deliveries = new AtomicLong();

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        CountDownLatch connected = new CountDownLatch(USERS);
        CountDownLatch finishedSending = new CountDownLatch(USERS);
        List<StompSession> sessions = java.util.Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < USERS; i++) {
            final int userIndex = i;
            final String roomId = roomIds.get(roomCount == 1 ? 0 : userIndex);
            pool.submit(() -> {
                try {
                    StompSession session = connect(prefix, userIndex, roomId, subscribe,
                            sentAtNanos, latenciesMicros, deliveries);
                    sessions.add(session);
                    connected.countDown();
                    // 모든 클라이언트가 붙은 뒤 동시에 시작한다.
                    connected.await(60, TimeUnit.SECONDS);
                    send(session, prefix, userIndex, roomId, messagesPerUser, sentAtNanos);
                } catch (Exception e) {
                    System.out.println("### [" + label + "] 사용자 " + userIndex + " 실패: " + e);
                    connected.countDown();
                } finally {
                    finishedSending.countDown();
                }
            });
        }

        connected.await(60, TimeUnit.SECONDS);
        long startedAt = System.currentTimeMillis();
        finishedSending.await(3, TimeUnit.MINUTES);

        // 서버가 실제로 일을 끝낸 시점 = 보낸 만큼 DB 에 저장된 시점.
        long ingestMillis = waitUntilPersisted(baseline + expectedSends, startedAt);

        pool.shutdownNow();
        sessions.forEach(s -> {
            try {
                s.disconnect();
            } catch (Exception ignored) {
                // 이미 끊긴 세션은 신경 쓰지 않는다.
            }
        });
        // 남은 전달 프레임이 다음 시나리오에 섞이지 않도록 잠시 쉰다.
        Thread.sleep(1_500);

        return new Result(label, expectedSends, deliveries.get(), ingestMillis,
                percentile(latenciesMicros, 50), percentile(latenciesMicros, 95));
    }

    /** 저장이 완료될 때까지 기다린다. count 쿼리 한 건이라 측정에 주는 부담이 작다. */
    private long waitUntilPersisted(long target, long startedAt) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 120_000;
        while (System.currentTimeMillis() < deadline) {
            if (chatMessageRepository.count() >= target) {
                return System.currentTimeMillis() - startedAt;
            }
            Thread.sleep(20);
        }
        return -1;
    }

    private static Double percentile(List<Long> micros, double p) {
        List<Long> sorted;
        synchronized (micros) {
            sorted = new ArrayList<>(micros);
        }
        if (sorted.isEmpty()) {
            return Double.NaN;
        }
        java.util.Collections.sort(sorted);
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1))) / 1000.0;
    }

    /** 시나리오마다 방과 계정을 새로 만든다. 이전 시나리오의 캐시·구독이 섞이지 않게 한다. */
    private List<String> seed(String prefix, int roomCount) {
        List<String> roomIds = new ArrayList<>();
        List<Team> teams = new ArrayList<>();

        for (int r = 0; r < roomCount; r++) {
            Team team = teamRepository.save(Team.builder().teamName(prefix + " 모임 " + r).build());
            String roomId = prefix + "-room-" + r;
            ChatRoom room = ChatRoom.builder().roomId(roomId).roomName(prefix + " 방 " + r).team(team).build();
            room.setTeam(team);
            chatRoomRepository.save(room);
            roomIds.add(roomId);
            teams.add(team);
        }

        for (int i = 0; i < USERS; i++) {
            Account account = accountRepository.save(Account.builder()
                    .email(prefix + i + "@eatmate.com").nickname(prefix + i).password("x").build());
            setOauth2Id(account, oauth2Id(prefix, i));
            accountRepository.save(account);
            // 구독에는 멤버십이 필요하다(ISS-01 조치).
            Team team = teams.get(roomCount == 1 ? 0 : i);
            accountTeamRepository.save(AccountTeam.builder()
                    .account(account).team(team).isLeader(i == 0).build());
        }
        return roomIds;
    }

    private String oauth2Id(String prefix, int index) {
        return prefix + "-oauth-" + index;
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

    private StompSession connect(String prefix, int userIndex, String roomId, boolean subscribe,
                                 Map<String, Long> sentAtNanos, List<Long> latenciesMicros,
                                 AtomicLong deliveries) throws Exception {
        String oauth2Id = oauth2Id(prefix, userIndex);
        String jwt = jwtTokenProvider.generateToken(oauth2Id, prefix + userIndex);
        String cookie = login(oauth2Id);

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        client.setTaskScheduler(scheduler);

        WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
        handshake.add(HttpHeaders.COOKIE, cookie);
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("token", jwt);

        StompSession session = client
                .connectAsync("ws://localhost:" + port + "/ws-stomp/websocket",
                        handshake, connectHeaders, new StompSessionHandlerAdapter() { })
                .get(30, TimeUnit.SECONDS);

        if (subscribe) {
            session.subscribe("/sub/chat/room/" + roomId, new StompFrameHandler() {
                @Override
                public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
                    return Map.class;
                }

                @Override
                @SuppressWarnings("unchecked")
                public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                    deliveries.incrementAndGet();
                    Object text = ((Map<String, Object>) payload).get("message");
                    Long sentAt = (text == null) ? null : sentAtNanos.remove(text.toString());
                    if (sentAt != null) {
                        latenciesMicros.add((System.nanoTime() - sentAt) / 1_000);
                    }
                }
            });
        }
        return session;
    }

    private void send(StompSession session, String prefix, int userIndex, String roomId,
                      int messagesPerUser, Map<String, Long> sentAtNanos) throws InterruptedException {
        StompHeaders headers = new StompHeaders();
        headers.setDestination("/pub/chat/message");
        headers.add("token", jwtTokenProvider.generateToken(oauth2Id(prefix, userIndex), prefix + userIndex));

        for (int m = 0; m < messagesPerUser; m++) {
            String text = prefix + "-u" + userIndex + "-m" + m;
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", ChatMessage.MessageType.TALK.name());
            payload.put("roomId", roomId);
            payload.put("message", text);

            sentAtNanos.put(text, System.nanoTime());
            session.send(headers, payload);
            Thread.sleep(SEND_INTERVAL_MILLIS);
        }
    }

    /** 쿠키 이름을 짐작하지 않고 Set-Cookie 를 그대로 되돌려 보낸다(세션이 Redis 라 SESSION 이다). */
    private String login(String oauth2Id) {
        ResponseEntity<String> response = rest.getForEntity(
                "http://localhost:" + port + "/test-only/login?oauth2Id=" + oauth2Id, String.class);
        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies == null || setCookies.isEmpty()) {
            throw new IllegalStateException("세션 쿠키를 받지 못했다");
        }
        return setCookies.stream().map(h -> h.split(";", 2)[0]).collect(Collectors.joining("; "));
    }
}
