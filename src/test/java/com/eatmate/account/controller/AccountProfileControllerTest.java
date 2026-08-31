package com.eatmate.account.controller;

import com.eatmate.account.service.AccountMyBatisService;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.post.service.PostTeamService;
import com.eatmate.team.service.TeamAccessService;
import com.eatmate.team.vo.TeamMembership;
import com.eatmate.weblogout.service.LogoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 프로필의 탈퇴 경로 인가(ISS-01).
 *
 * 팀 탈퇴와 회원 탈퇴는 둘 다 "누구를"을 요청 파라미터로 받고 세션 주체와 대조하지
 * 않았다. 남의 식별자를 실어 보내면 그 사람을 팀에서 빼내거나 계정을 지울 수 있었다.
 * 여기서 고정하는 것은 <b>대상은 언제나 세션 주체</b>라는 계약이다.
 */
@WebMvcTest(AccountProfileController.class)
class AccountProfileControllerTest {

    private static final Long TEAM_ID = 1L;
    private static final String ME = "my-oauth-id";
    private static final long MY_ACCOUNT_ID = 7L;

    @Autowired private MockMvc mockMvc;

    @MockBean private AccountMyBatisService accountMyBatisService;
    @MockBean private LogoutService logoutService;
    @MockBean private PostTeamService postTeamService;
    @MockBean private TeamAccessService teamAccessService;

    private TeamMembership membership(long accountId, boolean leader) {
        return new TeamMembership(accountId, leader);
    }

    @Test
    @WithMockUser(username = ME)
    @DisplayName("팀 탈퇴 대상은 파라미터가 아니라 세션 주체다")
    void leaveTeamIgnoresSuppliedAccountId() throws Exception {
        given(teamAccessService.requireMember(TEAM_ID, ME)).willReturn(membership(MY_ACCOUNT_ID, false));

        // 남의 account_id(999)를 실어 보내도 빠지는 것은 내 계정(7)이어야 한다.
        mockMvc.perform(post("/profile/leaveTeam").with(csrf())
                        .param("team_id", "1")
                        .param("account_id", "999"))
                .andExpect(status().is3xxRedirection());

        verify(postTeamService).kickMember("7", "1");
        verify(postTeamService, never()).kickMember("999", "1");
    }

    @Test
    @WithMockUser(username = ME)
    @DisplayName("멤버가 아닌 팀에서는 탈퇴할 수 없다")
    void nonMemberCannotLeaveTeam() throws Exception {
        given(teamAccessService.requireMember(TEAM_ID, ME))
                .willThrow(new AccessDeniedException("이 모임의 멤버가 아닙니다."));

        mockMvc.perform(post("/profile/leaveTeam").with(csrf()).param("team_id", "1"))
                .andExpect(status().isForbidden());

        verify(postTeamService, never()).kickMember(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = ME)
    @DisplayName("개설자는 팀에서 탈퇴할 수 없다")
    void leaderCannotLeaveTeam() throws Exception {
        given(teamAccessService.requireMember(TEAM_ID, ME)).willReturn(membership(MY_ACCOUNT_ID, true));

        // 통과시키면 리더 없는 팀이 남는다. 개설자는 탈퇴가 아니라 모임 삭제를 해야 한다.
        mockMvc.perform(post("/profile/leaveTeam").with(csrf()).param("team_id", "1"))
                .andExpect(status().isForbidden());

        verify(postTeamService, never()).kickMember(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = ME)
    @DisplayName("회원 탈퇴 대상은 파라미터가 아니라 세션 주체다")
    void deleteAccountIgnoresSuppliedIdentifiers() throws Exception {
        AccountDto me = new AccountDto();
        me.setAccount_id(MY_ACCOUNT_ID);
        me.setOauth2_id(ME);
        given(accountMyBatisService.findByOauth2Id(ME)).willReturn(me);

        // 남의 식별자를 실어 보내도 지워지는 것은 내 계정이어야 한다.
        mockMvc.perform(post("/profile/delete").with(csrf())
                        .param("oauth2_id", "victim-oauth-id")
                        .param("account_id", "999"))
                .andExpect(status().is3xxRedirection());

        verify(accountMyBatisService).deleteUserByOauth2Id(ME, "7");
        verify(accountMyBatisService, never()).deleteUserByOauth2Id("victim-oauth-id", "999");
    }

    @Test
    @DisplayName("인증되지 않으면 탈퇴할 수 없다")
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(post("/profile/leaveTeam").with(csrf()).param("team_id", "1"))
                .andExpect(status().is3xxRedirection());

        verify(postTeamService, never()).kickMember(anyString(), anyString());
    }
}
