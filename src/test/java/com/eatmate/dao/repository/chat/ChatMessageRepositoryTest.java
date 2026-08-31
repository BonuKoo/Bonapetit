package com.eatmate.dao.repository.chat;

import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TestEntityManager em;

    private ChatRoom roomA;
    private ChatRoom roomB;
    private Account sender;

    @BeforeEach
    void setUp() {
        sender = em.persist(Account.builder()
                .email("tester@eatmate.com")
                .nickname("테스터")
                .password("encoded-password")
                .build());

        roomA = em.persist(ChatRoom.builder()
                .roomId("room-a")
                .roomName("A팀 채팅방")
                .build());

        roomB = em.persist(ChatRoom.builder()
                .roomId("room-b")
                .roomName("B팀 채팅방")
                .build());
    }

    private ChatMessage save(ChatRoom room, String text) {
        return em.persist(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .senderName(sender.getNickname())
                .type(ChatMessage.MessageType.TALK)
                .message(text)
                .build());
    }

    @Test
    @DisplayName("최신 N건을 id 내림차순으로 정확히 N건 반환한다")
    void findLatest_returnsNewestFirstLimitedToSize() {
        for (int i = 1; i <= 10; i++) {
            save(roomA, "메시지 " + i);
        }
        em.flush();

        List<ChatMessage> result =
                chatMessageRepository.findLatestByRoomId("room-a", PageRequest.of(0, 4));

        assertThat(result).hasSize(4);
        assertThat(result).extracting(ChatMessage::getMessage)
                .containsExactly("메시지 10", "메시지 9", "메시지 8", "메시지 7");
        assertThat(result).extracting(ChatMessage::getId).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    @Test
    @DisplayName("커서로 다음 페이지를 가져오면 겹치지도 빠지지도 않는다")
    void findBefore_pagesWithoutOverlapOrGap() {
        for (int i = 1; i <= 10; i++) {
            save(roomA, "메시지 " + i);
        }
        em.flush();

        List<ChatMessage> firstPage =
                chatMessageRepository.findLatestByRoomId("room-a", PageRequest.of(0, 4));
        Long cursor = firstPage.get(firstPage.size() - 1).getId();

        List<ChatMessage> secondPage =
                chatMessageRepository.findByRoomIdBefore("room-a", cursor, PageRequest.of(0, 4));

        assertThat(secondPage).extracting(ChatMessage::getMessage)
                .containsExactly("메시지 6", "메시지 5", "메시지 4", "메시지 3");

        // 경계가 배타적이어야 한다. <= 였다면 커서 메시지("메시지 7")가 또 나온다.
        assertThat(secondPage).extracting(ChatMessage::getId).doesNotContain(cursor);

        // 두 페이지를 이으면 최신 8건이 빠짐없이 연속된다.
        List<String> merged = firstPage.stream().map(ChatMessage::getMessage).toList();
        assertThat(merged).containsExactly("메시지 10", "메시지 9", "메시지 8", "메시지 7");
    }

    @Test
    @DisplayName("다른 방의 메시지는 섞이지 않는다")
    void findLatest_isolatesByRoom() {
        save(roomA, "A방 메시지");
        save(roomB, "B방 메시지1");
        save(roomB, "B방 메시지2");
        em.flush();

        List<ChatMessage> result =
                chatMessageRepository.findLatestByRoomId("room-a", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("A방 메시지");
    }

    @Test
    @DisplayName("size+1로 조회하면 다음 페이지 존재 여부를 판정할 수 있다")
    void findLatest_sizePlusOneRevealsHasMore() {
        for (int i = 1; i <= 5; i++) {
            save(roomA, "메시지 " + i);
        }
        em.flush();

        // 3건을 보여주고 싶다면 4건을 조회한다 -> 4건이 오면 다음 페이지가 있다.
        List<ChatMessage> probe =
                chatMessageRepository.findLatestByRoomId("room-a", PageRequest.of(0, 4));
        assertThat(probe).hasSize(4);

        // 전체가 5건이므로 6건을 요청하면 5건만 온다 -> 다음 페이지가 없다.
        List<ChatMessage> all =
                chatMessageRepository.findLatestByRoomId("room-a", PageRequest.of(0, 6));
        assertThat(all).hasSize(5);
    }

    @Test
    @DisplayName("메시지가 없는 방은 빈 리스트를 반환한다")
    void findLatest_emptyRoomReturnsEmptyList() {
        List<ChatMessage> result =
                chatMessageRepository.findLatestByRoomId("room-a", PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("가장 오래된 메시지를 커서로 주면 빈 리스트를 반환한다")
    void findBefore_atOldestReturnsEmptyList() {
        ChatMessage oldest = save(roomA, "첫 메시지");
        save(roomA, "둘째 메시지");
        em.flush();

        List<ChatMessage> result = chatMessageRepository
                .findByRoomIdBefore("room-a", oldest.getId(), PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }
}
