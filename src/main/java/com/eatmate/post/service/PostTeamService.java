package com.eatmate.post.service;

import com.eatmate.chat.redisDao.ChatCacheRepository;
import com.eatmate.dao.mybatis.account.AccountTeamDao;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.domain.entity.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostTeamService {

    private final AccountTeamDao accountTeamDao;
    private final ChatRoomRepository chatRoomRepository;
    private final AccountRepository accountRepository;
    private final ChatCacheRepository chatCacheRepository;

    /**
     * 팀에서 특정 계정을 제외한다. 강퇴(리더가 실행)와 탈퇴(본인이 실행) 모두 이 경로를 쓴다.
     */
    public void kickMember(String account_id, String team_id) {
        // 1. account_team 테이블에서 해당 유저가 속한 팀과의 관계 삭제
        accountTeamDao.deleteAccountFromTeam(account_id, team_id);
        // 2. 채팅 내역 조회를 막기 위해 멤버십 캐시를 무효화
        evictChatMembership(account_id, team_id);
    }

    /**
     * 멤버십 캐시를 즉시 걷어낸다.
     *
     * 탈퇴·강퇴는 권한을 <b>빼앗는</b> 작업이라 TTL 만료만 기다리면 그 사이 내역 조회가
     * 계속 통과한다. 캐시 키가 (roomId, oauth2Id) 기준이므로 teamId -> roomId,
     * accountId -> oauth2Id 변환이 필요하다. 강퇴는 드문 작업이라 조회 2건은 부담이 아니다.
     *
     * 캐시 정리에 실패해도 탈퇴 자체는 이미 끝났으므로 예외를 전파하지 않는다.
     * 최악의 경우 TTL(5분)이 지나면 스스로 회복된다.
     */
    private void evictChatMembership(String accountId, String teamId) {
        try {
            ChatRoom room = chatRoomRepository.findByTeam(Long.parseLong(teamId));
            if (room == null) {
                return; // 채팅방이 없는 팀
            }
            accountRepository.findById(Long.parseLong(accountId))
                    .ifPresent(account ->
                            chatCacheRepository.evictMember(room.getRoomId(), account.getOauth2id()));

        } catch (Exception e) {
            log.warn("멤버십 캐시 무효화 실패 - TTL 만료를 기다린다. accountId={} teamId={}",
                    accountId, teamId, e);
        }
    }
}
