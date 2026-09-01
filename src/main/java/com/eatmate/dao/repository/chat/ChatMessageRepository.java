package com.eatmate.dao.repository.chat;

import com.eatmate.domain.entity.chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 채팅 내역 조회용 저장소.
 *
 * 커서 기반 페이징을 쓴다. Page/Slice 대신 List + Pageable(limit 용도)인 이유는
 * Page가 매 요청마다 count(*) 쿼리를 추가로 날리기 때문이다. 채팅 내역에 전체
 * 건수는 필요 없고, 다음 페이지 존재 여부는 size+1건을 조회해 판정하면 된다.
 *
 * 두 쿼리 모두 (room_id, chat_message_id) 복합 인덱스로 처리된다.
 * sender는 left join fetch로 N+1을 방지한다. ToOne 페치라 DB 레벨 페이징이
 * 그대로 적용된다(컬렉션 페치가 아니므로 메모리 페이징으로 떨어지지 않는다).
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 방의 최신 메시지 N건. 채팅방 최초 진입 시 사용한다.
     */
    @Query("select m from ChatMessage m " +
            "left join fetch m.sender " +
            "where m.chatRoom.roomId = :roomId " +
            "order by m.id desc")
    List<ChatMessage> findLatestByRoomId(@Param("roomId") String roomId, Pageable pageable);

    /**
     * 커서(beforeId)보다 이전 메시지 N건. 위로 스크롤할 때 사용한다.
     * 경계는 반드시 배타적(<)이어야 한다. <= 로 두면 페이지마다 1건씩 중복된다.
     */
    @Query("select m from ChatMessage m " +
            "left join fetch m.sender " +
            "where m.chatRoom.roomId = :roomId " +
            "and m.id < :beforeId " +
            "order by m.id desc")
    List<ChatMessage> findByRoomIdBefore(@Param("roomId") String roomId,
                                         @Param("beforeId") Long beforeId,
                                         Pageable pageable);

    /**
     * 방의 메시지를 전부 지운다. 모임 삭제 시 채팅방보다 먼저 실행해야 한다.
     *
     * <p>{@code chat_message.room_id}가 {@code nullable = false}라 연결만 끊을 수 없고
     * 방과 함께 지워야 한다. 이 단계가 없으면 FK 제약이 모임 삭제 자체를 막는다.
     *
     * <p>ChatRoom에 {@code @OneToMany(cascade = ALL)}을 다는 방법도 있지만 쓰지 않았다.
     * 그러면 방 삭제 때 메시지를 전부 영속성 컨텍스트에 올려 한 건씩 DELETE한다.
     * 대화가 많은 방일수록 비용이 선형으로 커진다. 여기서는 DELETE 한 문장이면 된다.
     */
    @Modifying
    @Query("delete from ChatMessage m where m.chatRoom.roomId = :roomId")
    int deleteByRoomId(@Param("roomId") String roomId);
}
