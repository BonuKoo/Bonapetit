package com.eatmate.dao.repository.team;

import com.eatmate.chat.dto.ChatRoomDTO;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class CustomTeamRepository {

    private final EntityManager entityManager;

    @Transactional
    public ChatRoomDTO createTeamAndChatRoomThenReturnChatRoomDto(Team team) {

        try {

            // Team 영속성 저장
            entityManager.persist(team);



            // 쿼리 발생 및 즉시 반영
            entityManager.flush();

            // 영속성 컨텍스트 초기화 (team과 관련된 엔티티는 비영속 상태가 됨)
            //entityManager.clear();

            // Team의 ID로 ChatRoom 직접 조회
            ChatRoom chatRoom = entityManager.createQuery(
                            "SELECT c FROM ChatRoom c WHERE c.team.id = :teamId", ChatRoom.class)
                    .setParameter("teamId", team.getId())
                    .getSingleResult();

            // ChatRoom을 Redis용 DTO로 변환 후 반환
            return chatRoom.toRedisDTO();

        } catch (PersistenceException e) {
            // persist/flush/조회 실패(제약 위반, ChatRoom 미존재 등). Spring의 트랜잭션
            // 관리가 롤백을 처리해 주므로 여기서는 원인 예외를 감싸 다시 던지기만 한다.
            throw new RuntimeException("Failed to create team and chat room", e);
        }
    }
}

