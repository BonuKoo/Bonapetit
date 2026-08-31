package com.eatmate.chat.service;

import com.eatmate.chat.dto.ChatHistoryResponse;
import com.eatmate.chat.dto.ChatMessageResponse;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chat.ChatMessageRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 채팅 내역 조회(읽기 경로).
 *
 * 실시간 발송은 ChatService가 담당한다. 읽기와 쓰기를 나눈 이유는 인가 검증과
 * 커서 계산이 붙으면서 한 클래스가 과하게 커지기 때문이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatHistoryService {

    static final int DEFAULT_SIZE = 50;
    static final int MAX_SIZE = 100;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final AccountRepository accountRepository;
    private final AccountTeamRepository accountTeamRepository;

    public ChatHistoryResponse getHistory(String roomId, Long before, Integer size, String oauth2Id) {
        verifyMembership(roomId, oauth2Id);

        int limit = clampSize(size);

        // size+1건을 조회해 다음 페이지 존재 여부를 판정한다. count 쿼리가 필요 없다.
        PageRequest probe = PageRequest.of(0, limit + 1);
        List<ChatMessage> found = (before == null)
                ? chatMessageRepository.findLatestByRoomId(roomId, probe)
                : chatMessageRepository.findByRoomIdBefore(roomId, before, probe);

        boolean hasMore = found.size() > limit;
        List<ChatMessage> page = hasMore ? found.subList(0, limit) : found;

        // 조회는 id 내림차순이므로 마지막 원소가 이 페이지에서 가장 오래된 메시지다.
        Long nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).getId();

        List<ChatMessageResponse> messages = new ArrayList<>(
                page.stream().map(ChatMessageResponse::from).toList());
        // 화면은 시간순으로 그리므로 오래된 것 -> 최신 순으로 뒤집는다.
        Collections.reverse(messages);

        return new ChatHistoryResponse(messages, nextCursor, hasMore);
    }

    /**
     * size 상한을 두지 않으면 size=100000 같은 요청으로 방 전체를 한 번에 긁을 수 있다.
     */
    private int clampSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 방이 속한 팀의 멤버만 내역을 볼 수 있다.
     *
     * 참고: 기존 ChatRoomController.enterRoom은 teamId만 받고 멤버십을 확인하지 않아
     * 남의 팀 채팅방 정보를 조회할 수 있다. 그건 이 변경의 범위 밖이라 손대지 않았다.
     */
    private void verifyMembership(String roomId, String oauth2Id) {
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
    }
}
