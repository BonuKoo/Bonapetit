package com.eatmate.post.controller;

import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.post.service.PostJpaService;
import com.eatmate.post.service.PostTeamService;
import com.eatmate.team.service.TeamAccessService;
import com.eatmate.team.service.TeamJpaService;
import com.eatmate.team.vo.TeamMembership;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 모임 관리 인가(ISS-01).
 *
 * 이전에는 로그인만 했으면 teamId를 실어 보내는 것만으로 남의 모임을 고치고 지우고
 * 팀원을 강퇴할 수 있었다. 여기서 고정하는 것은 "요청 파라미터가 아니라 세션 주체로
 * 판단한다"는 계약이다.
 */
@WebMvcTest(PostController.class)
class PostControllerTest {

    private static final Long TEAM_ID = 1L;
    private static final String LEADER = "leader-oauth-id";
    private static final String OUTSIDER = "outsider-oauth-id";

    @Autowired private MockMvc mockMvc;

    @MockBean private PostJpaService postJpaService;
    @MockBean private TeamJpaService teamJpaService;
    @MockBean private TeamRepository teamRepository;
    @MockBean private PostTeamService postTeamService;
    @MockBean private TeamAccessService teamAccessService;

    /** 개설자 멤버십. account_id는 요청이 아니라 여기서 나온 값을 써야 한다. */
    private void givenLeader(String oauth2Id, long accountId) {
        given(teamAccessService.requireLeader(TEAM_ID, oauth2Id))
                .willReturn(new TeamMembership(accountId, true));
    }

    private void givenNotLeader(String oauth2Id) {
        given(teamAccessService.requireLeader(eq(TEAM_ID), eq(oauth2Id)))
                .willThrow(new AccessDeniedException("이 모임의 개설자만 할 수 있는 작업입니다."));
    }

    @Test
    @WithMockUser(username = OUTSIDER)
    @DisplayName("개설자가 아니면 모임을 수정할 수 없다")
    void outsiderCannotUpdateTeam() throws Exception {
        givenNotLeader(OUTSIDER);

        mockMvc.perform(post("/post/updateTeam").with(csrf())
                        .param("teamId", "1")
                        .param("teamName", "가로챈 이름")
                        .param("description", "설명"))
                .andExpect(status().isForbidden());

        verify(postJpaService, never()).updateTeam(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser(username = LEADER)
    @DisplayName("개설자는 모임을 수정할 수 있다")
    void leaderCanUpdateTeam() throws Exception {
        givenLeader(LEADER, 7L);

        mockMvc.perform(post("/post/updateTeam").with(csrf())
                        .param("teamId", "1")
                        .param("teamName", "새 이름")
                        .param("description", "설명"))
                .andExpect(status().is3xxRedirection());

        verify(postJpaService).updateTeam(eq(TEAM_ID), eq("새 이름"), eq("설명"), any());
    }

    @Test
    @WithMockUser(username = OUTSIDER)
    @DisplayName("개설자가 아니면 모임을 삭제할 수 없다")
    void outsiderCannotDeleteTeam() throws Exception {
        givenNotLeader(OUTSIDER);

        mockMvc.perform(post("/post/deleteTeam").with(csrf()).param("teamId", "1"))
                .andExpect(status().isForbidden());

        verify(teamRepository, never()).delete(any());
    }

    @Test
    @WithMockUser(username = LEADER)
    @DisplayName("개설자는 모임을 삭제할 수 있다")
    void leaderCanDeleteTeam() throws Exception {
        givenLeader(LEADER, 7L);
        Team team = Team.builder().id(TEAM_ID).teamName("A팀").build();
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team));

        mockMvc.perform(post("/post/deleteTeam").with(csrf()).param("teamId", "1"))
                .andExpect(status().is3xxRedirection());

        verify(teamRepository).delete(team);
    }

    @Test
    @WithMockUser(username = OUTSIDER)
    @DisplayName("개설자가 아니면 팀원을 강퇴할 수 없다")
    void outsiderCannotKickMember() throws Exception {
        givenNotLeader(OUTSIDER);

        mockMvc.perform(post("/post/kickMember").with(csrf())
                        .param("account_id", "9")
                        .param("team_id", "1"))
                .andExpect(status().isForbidden());

        verify(postTeamService, never()).kickMember(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = LEADER)
    @DisplayName("개설자는 다른 팀원을 강퇴할 수 있다")
    void leaderCanKickMember() throws Exception {
        givenLeader(LEADER, 7L);

        mockMvc.perform(post("/post/kickMember").with(csrf())
                        .param("account_id", "9")
                        .param("team_id", "1"))
                .andExpect(status().is3xxRedirection());

        verify(postTeamService).kickMember("9", "1");
    }

    @Test
    @WithMockUser(username = LEADER)
    @DisplayName("개설자가 자기 자신을 강퇴하면 거부한다")
    void leaderCannotKickSelf() throws Exception {
        givenLeader(LEADER, 7L);

        // 화면에는 자기 강퇴 버튼이 없지만 요청은 직접 만들 수 있다.
        // 통과시키면 리더 없는 팀이 남아 목록에서 사라지고 아무도 관리할 수 없게 된다.
        mockMvc.perform(post("/post/kickMember").with(csrf())
                        .param("account_id", "7")
                        .param("team_id", "1"))
                .andExpect(status().isForbidden());

        verify(postTeamService, never()).kickMember(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = OUTSIDER)
    @DisplayName("개설자가 아니면 팀원 명단을 볼 수 없다")
    void outsiderCannotViewMembers() throws Exception {
        givenNotLeader(OUTSIDER);

        mockMvc.perform(get("/post/members/{teamId}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OUTSIDER)
    @DisplayName("개설자가 아니면 수정 화면을 볼 수 없다")
    void outsiderCannotOpenUpdateForm() throws Exception {
        givenNotLeader(OUTSIDER);

        mockMvc.perform(get("/post/update/{teamId}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증되지 않으면 모임을 수정할 수 없다")
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(post("/post/updateTeam").with(csrf()).param("teamId", "1")
                        .param("teamName", "x").param("description", "y"))
                .andExpect(status().is3xxRedirection());
    }
}
