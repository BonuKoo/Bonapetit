package com.eatmate.team.service;

import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 대화가 오간 모임을 삭제할 수 있는가.
 *
 * Team 은 cascade = ALL 로 AccountTeam 과 ChatRoom 을 함께 지운다. 그런데
 * ChatRoom 에는 메시지 컬렉션이 없어 cascade 가 chat_message 까지 닿지 않고,
 * chat_message.room_id 는 nullable = false 다.
 *
 * 실측 결과 <b>걸린다.</b> 대화가 오간 모임은 삭제되지 않는다.
 *
 * <pre>
 * ConstraintViolationException: CHAT_MESSAGE FOREIGN KEY(ROOM_ID) REFERENCES CHAT_ROOM(ROOM_ID)
 * </pre>
 *
 * ISS-15(탈퇴)와 같은 모양이다. 새 테이블이 생겼는데 삭제 경로가 따라오지 않았다.
 * 다만 처방은 다르다. chat_message.room_id 는 nullable = false 라 NULL 로 끊을 수 없고,
 * 방과 함께 지워야 한다.
 *
 * 이 테스트는 현재 동작을 못 박은 것이다. 고치면 실패하므로 놓칠 수 없다.
 */
@DataJpaTest
@ActiveProfiles("test")
class TeamDeletionFkTest {

    @Autowired private TeamRepository teamRepository;
    @Autowired private TestEntityManager em;

    private Long teamId;

    @BeforeEach
    void setUp() {
        Account account = em.persist(Account.builder()
                .email("tester@eatmate.com").nickname("테스터").password("x").build());

        Team team = Team.builder().teamName("사라질 모임").build();
        ChatRoom room = ChatRoom.builder().roomId("room-doomed").roomName("사라질 모임").build();
        team.setChatRoom(room);
        room.setTeam(team);
        team.addAccountTeam(AccountTeam.builder().account(account).isLeader(true).build());
        em.persist(team);
        em.persist(room);

        em.persist(ChatMessage.builder()
                .chatRoom(room).sender(account).senderName("테스터")
                .type(ChatMessage.MessageType.TALK).message("여기 대화가 있었다").build());

        em.flush();
        teamId = team.getId();
        em.clear();
    }

    @Test
    @DisplayName("대화가 오간 모임은 FK 제약에 걸려 삭제되지 않는다")
    void deletingTeamWithMessagesFails() {
        Team team = teamRepository.findById(teamId).orElseThrow();

        assertThatThrownBy(() -> {
            teamRepository.delete(team);
            em.flush();
        })
                .as("Team 의 cascade 는 ChatRoom 까지만 닿고 chat_message 는 남는다")
                .isInstanceOf(ConstraintViolationException.class);
    }
}
