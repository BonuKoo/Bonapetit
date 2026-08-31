package com.eatmate.team.service;

import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.team.vo.TeamMembership;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모임(팀) 단위 인가 검사.
 *
 * 모임 수정·삭제·강퇴·탈퇴는 "로그인했는가"가 아니라 "이 모임에서 무엇인가"로 갈린다.
 * 그 판단을 컨트롤러마다 되풀이하면 한 곳만 빠뜨려도 곧 취약점이 되므로 한군데에 모은다.
 *
 * <h3>왜 oauth2Id로 묻는가</h3>
 * 화면이 넘겨주는 account_id·team_id는 사용자가 바꿔 보낼 수 있는 값이다. 세션 주체에서
 * 얻은 oauth2Id({@code Principal.getName()})만이 위조되지 않으므로, 인가 검사는 항상
 * 이 값을 기준으로 한다. 요청 파라미터는 "무엇을"에만 쓰고 "누가"에는 쓰지 않는다.
 *
 * <h3>실패 시 예외</h3>
 * 멤버가 아니면 {@link AccessDeniedException}이다. "그런 모임이 없다"와 "권한이 없다"를
 * 구분해 알려주면 모임 존재 여부를 훑을 수 있으므로 둘 다 같은 예외로 막는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamAccessService {

    private final AccountTeamRepository accountTeamRepository;

    /**
     * 요청자가 해당 모임의 멤버임을 확인하고 그 멤버십을 돌려준다.
     *
     * 반환값을 쓰는 쪽은 여기서 얻은 account_id·is_leader를 신뢰할 수 있다.
     * 요청 파라미터로 들어온 account_id 대신 이 값을 쓰는 것이 요점이다.
     */
    public TeamMembership requireMember(Long teamId, String oauth2Id) {
        if (teamId == null || oauth2Id == null) {
            throw new AccessDeniedException("인증 정보가 없습니다.");
        }
        return accountTeamRepository.findMembership(teamId, oauth2Id)
                .orElseThrow(() -> new AccessDeniedException("이 모임의 멤버가 아닙니다."));
    }

    /**
     * 요청자가 해당 모임의 개설자임을 확인한다.
     */
    public TeamMembership requireLeader(Long teamId, String oauth2Id) {
        TeamMembership membership = requireMember(teamId, oauth2Id);
        if (!membership.leader()) {
            throw new AccessDeniedException("이 모임의 개설자만 할 수 있는 작업입니다.");
        }
        return membership;
    }
}
