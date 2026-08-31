package com.eatmate.chat.service;

import com.eatmate.chat.dto.ChatHistoryResponse;
import com.eatmate.chat.dto.ChatMessageResponse;
import com.eatmate.chat.redisDao.ChatCacheRepository;
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
import java.util.Optional;

/**
 * 채팅 내역 조회(읽기 경로).
 *
 * 실시간 발송은 ChatService가 담당한다. 읽기와 쓰기를 나눈 이유는 인가 검증과
 * 커서 계산이 붙으면서 한 클래스가 과하게 커지기 때문이다.
 *
 * <h3>캐시 적용 범위</h3>
 * 방 진입(첫 페이지)만 캐시를 탄다. 위로 스크롤하는 구간은 항상 DB를 조회한다.
 * 첫 페이지만 캐싱하면 캐시와 DB의 경계가 "before 파라미터 유무" 하나로 명확해져,
 * 두 데이터 소스에 걸친 커서 계산이라는 까다로운 문제를 피할 수 있다.
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
    private final ChatCacheRepository chatCacheRepository;

    public ChatHistoryResponse getHistory(String roomId, Long before, Integer size, String oauth2Id) {
        verifyMembership(roomId, oauth2Id);

        int limit = clampSize(size);
        // 다음 페이지 존재 여부를 판정하려면 보여줄 개수보다 1건 더 필요하다.
        int probeSize = limit + 1;

        if (before == null && probeSize <= ChatCacheRepository.RECENT_CAPACITY) {
            return firstPageWithCache(roomId, limit, probeSize);
        }
        return fromDatabase(roomId, before, limit, probeSize);
    }

    /**
     * 방 진입 시의 첫 페이지. 캐시를 먼저 보고, 없으면 DB에서 읽어 캐시를 채운다.
     */
    private ChatHistoryResponse firstPageWithCache(String roomId, int limit, int probeSize) {
        Optional<List<ChatMessageResponse>> cached = chatCacheRepository.getRecent(roomId, probeSize);
        if (cached.isPresent()) {
            return buildResponse(cached.get(), limit);
        }

        // 캐시 미스. 요청분만 읽어 채우면 반쪽짜리 캐시가 되므로 용량만큼 통째로 읽는다.
        List<ChatMessageResponse> warmed = chatMessageRepository
                .findLatestByRoomId(roomId, PageRequest.of(0, ChatCacheRepository.RECENT_CAPACITY))
                .stream()
                .map(ChatMessageResponse::from)
                .toList();

        chatCacheRepository.warmRecent(roomId, warmed);

        return buildResponse(warmed.subList(0, Math.min(probeSize, warmed.size())), limit);
    }

    /**
     * 스크롤 구간, 또는 캐시 용량을 넘는 요청. 항상 DB를 조회한다.
     */
    private ChatHistoryResponse fromDatabase(String roomId, Long before, int limit, int probeSize) {
        PageRequest probe = PageRequest.of(0, probeSize);
        List<ChatMessage> found = (before == null)
                ? chatMessageRepository.findLatestByRoomId(roomId, probe)
                : chatMessageRepository.findByRoomIdBefore(roomId, before, probe);

        return buildResponse(found.stream().map(ChatMessageResponse::from).toList(), limit);
    }

    /**
     * id 내림차순 목록(최대 limit+1건)을 응답으로 변환한다.
     *
     * @param probe limit보다 1건 많으면 다음 페이지가 있다는 뜻이다.
     */
    private ChatHistoryResponse buildResponse(List<ChatMessageResponse> probe, int limit) {
        boolean hasMore = probe.size() > limit;
        List<ChatMessageResponse> page = hasMore ? probe.subList(0, limit) : probe;

        // 내림차순이므로 마지막 원소가 이 페이지에서 가장 오래된 메시지다.
        Long nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).id();

        List<ChatMessageResponse> messages = new ArrayList<>(page);
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
     * 캐시가 없던 때 이 검증에만 DB 쿼리 3건이 들었다(방 조회, 계정 조회, 멤버십 조회).
     * 방 진입마다 반복되는 데다 결과가 거의 바뀌지 않아 캐시 효과가 크다.
     * 멤버로 확인된 경우만 캐싱한다. 비멤버까지 캐싱하면 방금 참여한 사용자가
     * TTL이 끝날 때까지 차단된다.
     *
     * 참고: 기존 ChatRoomController.enterRoom은 teamId만 받고 멤버십을 확인하지 않아
     * 남의 팀 채팅방 정보를 조회할 수 있다. 그건 이 변경의 범위 밖이라 손대지 않았다.
     */
    private void verifyMembership(String roomId, String oauth2Id) {
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
