package com.eatmate.chat.service;

import com.eatmate.chat.redisDao.ChatCacheRepository;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 채팅방 멤버십 검증.
 *
 * 원래 ChatHistoryService의 private 메서드였다. 내역 조회(HTTP)뿐 아니라 STOMP
 * SUBSCRIBE에서도 같은 검증이 필요해지면서 별도 협력자로 꺼냈다. 두 경로가 같은
 * 코드를 쓰지 않으면 한쪽만 고쳐지는 일이 생기는데, 인가는 그게 곧 구멍이다.
 *
 * <h3>왜 roomId로 묻는가</h3>
 * {@link com.eatmate.team.service.TeamAccessService}는 teamId를 받는다. 채팅 경로가
 * 아는 것은 roomId뿐이고, roomId -> team 해석 자체가 검증의 일부(팀이 연결되지 않은
 * 방은 아무도 못 본다)라 별도로 둔다.
 *
 * <h3>캐시</h3>
 * 검증 결과는 Redis에 5분간 남는다. 캐시가 없던 때 이 검증에만 DB 쿼리 3건이 들었고,
 * 방 진입마다 반복되는 데다 결과가 거의 바뀌지 않아 효과가 크다(ADR-005).
 * <b>멤버로 확인된 경우만</b> 캐싱한다. 비멤버까지 캐싱하면 방금 참여한 사용자가
 * TTL이 끝날 때까지 차단된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomMembershipVerifier {

    private final ChatRoomRepository chatRoomRepository;
    private final AccountRepository accountRepository;
    private final AccountTeamRepository accountTeamRepository;
    private final ChatCacheRepository chatCacheRepository;

    /**
     * 방이 속한 팀의 멤버가 아니면 예외를 던진다.
     *
     * @throws ResponseStatusException 존재하지 않는 방(404)
     * @throws AccessDeniedException   팀이 없는 방, 알 수 없는 계정, 비멤버
     */
    public void verify(String roomId, String oauth2Id) {
        if (roomId == null || oauth2Id == null) {
            throw new AccessDeniedException("인증 정보가 없습니다.");
        }

        if (chatCacheRepository.isMemberVerified(roomId, oauth2Id)) {
            return;
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."));

        if (room.getTeam() == null) {
            throw new AccessDeniedException("팀이 연결되지 않은 채팅방입니다.");
        }

        Account account = accountRepository.findByOauth2id(oauth2Id)
                .orElseThrow(() -> new AccessDeniedException("계정을 찾을 수 없습니다."));

        accountTeamRepository.findByAccountAndTeam(account, room.getTeam())
                .orElseThrow(() -> new AccessDeniedException("이 채팅방의 멤버가 아닙니다."));

        chatCacheRepository.markMemberVerified(roomId, oauth2Id);
    }
}
