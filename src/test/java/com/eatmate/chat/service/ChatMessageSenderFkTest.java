package com.eatmate.chat.service;

import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.Team;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 메시지를 보낸 적 있는 계정은 지워지지 않는다.
 *
 * AccountMyBatisService.deleteUserByOauth2Id 는 notice 와 account_team 만 정리하고
 * chat_message 는 손대지 않는다. chat_message.account_id 가 account 를 참조하므로
 * FK 제약에 걸린다.
 *
 * <p>즉 <b>대화에 한 번이라도 참여한 사용자는 회원 탈퇴가 실패한다.</b>
 * 메시지 영속화(PR #70)가 들어오면서 생긴 회귀이며, 아직 고치지 않았다.
 * 이 테스트는 고쳐지면 실패하도록 현재 동작을 못 박아 둔 것이다.
 *
 * <p>고칠 때의 선택지는 두 가지다. 어느 쪽이든 sender_name 스냅샷이 있어 과거 대화의
 * 표시 이름은 남는다.
 * <ul>
 *   <li>삭제 경로에서 chat_message.account_id 를 NULL 로 바꾼다(sender 가 nullable 인 이유)</li>
 *   <li>FK 에 ON DELETE SET NULL 을 건다 — 다만 ddl-auto: update 로는 반영되지 않는다</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
class ChatMessageSenderFkTest {

    @Autowired private TestEntityManager em;

    @Test
    @DisplayName("메시지를 남긴 계정은 FK 제약에 걸려 삭제되지 않는다")
    void deletingAccountThatSentMessages() {
        Account account = em.persist(Account.builder()
                .email("tester@eatmate.com").nickname("테스터").password("x").build());
        Team team = em.persist(Team.builder().teamName("A팀").build());
        ChatRoom room = ChatRoom.builder().roomId("room-1").roomName("A팀 채팅방").team(team).build();
        em.persist(room);
        em.persist(ChatMessage.builder()
                .chatRoom(room).sender(account).senderName("테스터")
                .type(ChatMessage.MessageType.TALK).message("안녕하세요").build());
        em.flush();

        // MyBatis 삭제 경로와 같은 일을 한다 - account 행만 지운다.
        assertThatThrownBy(() -> em.getEntityManager()
                .createNativeQuery("DELETE FROM account WHERE account_id = :id")
                .setParameter("id", account.getId())
                .executeUpdate())
                .as("chat_message.account_id 가 FK 제약으로 삭제를 막는다")
                .isInstanceOf(ConstraintViolationException.class);
    }
}
