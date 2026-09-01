package com.eatmate.post.service;

import com.eatmate.chat.dto.ChatRoomDTO;
import com.eatmate.chat.redisDao.ChatCacheRepository;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.chat.service.ChatRoomService;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.chat.ChatMessageRepository;
import com.eatmate.dao.repository.team.CustomTeamRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.map.vo.MapVo;
import com.eatmate.post.vo.PostForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostJpaService {

    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;

    private final CustomTeamRepository customTeamRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatCacheRepository chatCacheRepository;
    private final ChatRoomRedisRepository chatRoomRedisRepository;

    private final ChatRoomService chatRoomService;

    /**
     * 게시글 생성
     */

    @Transactional
     public void createChatRoomAndTeamWhenWriteThePost(PostForm form, MapVo mapVo){

        Account account = accountRepository.findByOauth2id(form.getAuthor())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다: " + form.getAuthor()));

        //팀 만들기
         Team team = Team.builder()
                 .teamName(form.getTeamName())
                 .description(form.getDescription())
                 .mapId(mapVo.getMapId())
                 .addressName(mapVo.getAddressName())
                 .phone(mapVo.getPhone())
                 .placeName(mapVo.getPlaceName())
                 .placeUrl(mapVo.getPlaceUrl())
                 .roadAddressName(mapVo.getRoadAddressName())
                 .x(mapVo.getX())
                 .y(mapVo.getY())
                 .build();

         //팀에 할당되는 ChatRoom 생성

        /*
        StringBuffer stringBuffer = new StringBuffer();
        String chatRoomName = stringBuffer.append(form.getTeamName()).append(" 의 채팅방").toString();
         */

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(UUID.randomUUID().toString())
                .roomName(form.getTeamName())
                .build();

        AccountTeam accountTeam = AccountTeam.builder()
                 .account(account)
                 //.team(team)
                 .isLeader(true)
                 .build();

        team.addAccountTeam(accountTeam);
        team.setChatRoom(chatRoom);
        chatRoom.setTeam(team);

        ChatRoomDTO chatRoomDTO = customTeamRepository.createTeamAndChatRoomThenReturnChatRoomDto(team);

        chatRoomService.connectAndCreateChatRoom(chatRoomDTO);

    }

    /**
     * 모임 삭제.
     *
     * <p>Team은 cascade = ALL로 AccountTeam과 ChatRoom을 함께 지우지만, ChatRoom에는
     * 메시지 컬렉션이 없어 cascade가 chat_message까지 닿지 않는다. 그래서 대화가 한 번이라도
     * 오간 모임은 FK 제약에 걸려 삭제되지 않았다. 메시지를 먼저 지운다.
     *
     * <p>탈퇴({@code chat_message.account_id}를 NULL로)와 처방이 다른 이유는
     * {@code room_id}가 {@code nullable = false}이기 때문이다. 대화의 주인인 모임 자체가
     * 사라지므로 함께 지우는 것이 맞기도 하다.
     *
     * <h3>왜 컨트롤러가 아니라 여기인가</h3>
     * 삭제가 여러 단계가 되면서 한 트랜잭션으로 묶어야 한다. 중간에 실패해 메시지만
     * 지워지고 모임이 남으면 되돌릴 방법이 없다.
     *
     * <h3>캐시</h3>
     * 모임 삭제는 참여자 <b>전원에게서</b> 권한을 빼앗는 작업이다. 강퇴·탈퇴와 같은 이유로
     * TTL 만료를 기다리지 않고 즉시 무효화한다(ADR-005). 그러지 않으면 멤버십 캐시가
     * 살아 있는 동안 인가를 통과해 버리고, 방 조회를 건너뛰므로 "그런 방 없음"을 확인할
     * 기회조차 없다. 사라진 모임의 대화가 최대 5분간 계속 읽힌다.
     *
     * <p>캐시 정리는 DB 작업 뒤에 한다. 먼저 지우면 커밋 전에 다른 요청이 DB에서 다시
     * 캐시를 채워 되살아날 수 있다. 실패해도 예외를 전파하지 않는다 — 삭제는 이미
     * 끝났고, 캐시 때문에 삭제를 되돌릴 이유는 없다(ADR-006).
     */
    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid team ID: " + teamId));

        ChatRoom chatRoom = team.getChatRoom();
        String roomId = (chatRoom != null) ? chatRoom.getRoomId() : null;

        // 캐시 무효화에 필요한 식별자를 삭제 전에 확보한다.
        List<String> memberOauth2Ids = team.getMembers().stream()
                .map(accountTeam -> accountTeam.getAccount().getOauth2id())
                .filter(Objects::nonNull)
                .toList();

        if (roomId != null) {
            // 방보다 메시지가 먼저다. 순서가 바뀌면 FK 제약에 걸린다.
            chatMessageRepository.deleteByRoomId(roomId);
        }
        teamRepository.delete(team);

        if (roomId != null) {
            evictChatCaches(roomId, memberOauth2Ids);
        }
    }

    private void evictChatCaches(String roomId, List<String> memberOauth2Ids) {
        chatCacheRepository.evictRecent(roomId);
        memberOauth2Ids.forEach(oauth2Id -> chatCacheRepository.evictMember(roomId, oauth2Id));
        // TTL이 없는 해시라 지우지 않으면 사라진 방이 목록에 영원히 남는다.
        chatRoomRedisRepository.deleteChatRoom(roomId);
    }

    public void updateTeam(Long teamId, String teamName, String description, MapVo mapVo) {
        Team team = teamRepository.findById(teamId).orElse(null);

        if (team != null) {
            team.updateTeam(teamName, description, mapVo);
            teamRepository.save(team);
        }
    }
}
