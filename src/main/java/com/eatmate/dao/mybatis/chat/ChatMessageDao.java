package com.eatmate.dao.mybatis.chat;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatMessageDao {

    /**
     * 계정 삭제에 앞서 대화의 발신자 연결만 끊는다.
     *
     * 메시지를 지우지 않는 것은 설계 의도다. {@code ChatMessage.sender}가 nullable이고
     * 표시 이름을 {@code sender_name} 스냅샷으로 따로 들고 있는 이유가 이것이다.
     * 한 사람이 탈퇴했다고 남들이 나눈 대화의 맥락까지 사라지면 안 된다.
     *
     * <p>이 단계가 없으면 FK 제약이 계정 삭제 자체를 막는다. 즉
     * <b>대화에 한 번이라도 참여한 사용자는 탈퇴가 실패한다.</b>
     */
    @Update("UPDATE chat_message SET account_id = NULL WHERE account_id = #{account_id}")
    int detachSenderByAccountId(@Param("account_id") String account_id);
}
