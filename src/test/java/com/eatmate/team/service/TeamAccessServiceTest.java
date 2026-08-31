package com.eatmate.team.service;

import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.team.vo.TeamMembership;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TeamAccessServiceTest {

    private static final Long TEAM_ID = 1L;
    private static final String OAUTH_ID = "oauth-1";

    @Mock private AccountTeamRepository accountTeamRepository;

    @InjectMocks private TeamAccessService teamAccessService;

    private void givenMembership(boolean leader) {
        given(accountTeamRepository.findMembership(TEAM_ID, OAUTH_ID))
                .willReturn(Optional.of(new TeamMembership(7L, leader)));
    }

    @Test
    @DisplayName("멤버면 멤버십을 돌려준다")
    void memberGetsMembership() {
        givenMembership(false);

        TeamMembership membership = teamAccessService.requireMember(TEAM_ID, OAUTH_ID);

        // 호출부는 요청 파라미터가 아니라 여기서 얻은 account_id를 써야 한다.
        assertThat(membership.accountId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("멤버가 아니면 거부한다")
    void nonMemberIsDenied() {
        given(accountTeamRepository.findMembership(TEAM_ID, OAUTH_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> teamAccessService.requireMember(TEAM_ID, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("개설자면 리더 검사를 통과한다")
    void leaderPassesLeaderCheck() {
        givenMembership(true);

        assertThat(teamAccessService.requireLeader(TEAM_ID, OAUTH_ID).leader()).isTrue();
    }

    @Test
    @DisplayName("멤버지만 개설자가 아니면 리더 검사에서 거부한다")
    void memberIsNotLeader() {
        givenMembership(false);

        // 이게 ISS-01의 핵심이다. 로그인만 했으면 남의 모임을 고칠 수 있었다.
        assertThatThrownBy(() -> teamAccessService.requireLeader(TEAM_ID, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("인증 주체가 없으면 DB를 보지 않고 거부한다")
    void missingPrincipalIsDenied() {
        assertThatThrownBy(() -> teamAccessService.requireMember(TEAM_ID, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> teamAccessService.requireMember(null, OAUTH_ID))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(accountTeamRepository);
    }
}
