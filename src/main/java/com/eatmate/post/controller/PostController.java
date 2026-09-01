package com.eatmate.post.controller;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.map.vo.MapVo;
import com.eatmate.post.service.PostTeamService;
import com.eatmate.post.vo.PostForm;
import com.eatmate.post.service.PostJpaService;
import com.eatmate.team.service.TeamAccessService;
import com.eatmate.team.vo.TeamMembership;
import com.eatmate.team.service.TeamJpaService;
import com.eatmate.team.vo.TeamVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("post")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostJpaService postJpaService;
    private final TeamJpaService teamJpaService;
    private final TeamRepository teamRepository;

    private final PostTeamService postTeamService;
    private final TeamAccessService teamAccessService;

    // Create Post 페이지
    @GetMapping("/create")
    String createPost(Model model){
        PostForm form = new PostForm();
        model.addAttribute("createPost", form);
        return "post/createPostForm";
    }

    // 게시글 및 팀 생성
    @PostMapping("/createPost")
    public String createPost(RedirectAttributes redirectAttributes,
                             Principal principal,
                             PostForm postForm,
                             MapVo mapVo) throws IOException{
        postForm.addAuthor(principal.getName());
        postJpaService.createChatRoomAndTeamWhenWriteThePost(postForm, mapVo);
        redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 생성되었습니다.");
        return "redirect:/post/list";
    }

    // Team 목록 조회
    @GetMapping("/list")
    String listPosts(@RequestParam(name = "page", defaultValue = "1") int page,
                     @RequestParam(name = "keyword", defaultValue = "") String keyword,
                     Model model) {
        Page<TeamVo> list = teamJpaService.getList(page, keyword);

        int startPage = ((page - 1) / list.getSize()) * list.getSize() + 1;
        int endPage = startPage + list.getSize() - 1;
        if (endPage > list.getTotalPages()) {
            endPage = list.getTotalPages();
        }
        int lastPage = list.getTotalPages();

        model.addAttribute("teams", list);
        model.addAttribute("keyword", keyword);
        model.addAttribute("page", page);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("lastPage", lastPage);
        return "post/listPost";
    }

    // Team 상세 페이지
    @GetMapping("/detail/{teamId}")
    public String details(@PathVariable Long teamId, Model model) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid team ID: " + teamId));
        model.addAttribute("team", team);
        return "post/detailPostForm";
    }


    // 팀 수정 페이지
    @GetMapping("/update/{teamId}")
    public String crystals(@PathVariable Long teamId, Model model, Principal principal) {
        teamAccessService.requireLeader(teamId, principal.getName());

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid team ID: " + teamId));
        model.addAttribute("team", team);
        return "post/updatePostForm";
    }

    // 팀 수정 처리
    @PostMapping("/updateTeam")
    public String updateTeam(@RequestParam Long teamId,
                             @RequestParam String teamName,
                             @RequestParam String description,
                             MapVo mapVo,
                             Model model,
                             Principal principal) {
        // teamId는 요청이 주는 값이라 그대로 믿을 수 없다. 개설자인지 먼저 확인한다.
        teamAccessService.requireLeader(teamId, principal.getName());

        postJpaService.updateTeam(teamId, teamName, description, mapVo);

        // 수정 후 해당 페이지로 리다이렉트
        return "redirect:/post/detail/" + teamId;
    }

    // 팀 삭제 처리
    @PostMapping("/deleteTeam")
    public String deleteTeam(@RequestParam Long teamId, Principal principal) {
        teamAccessService.requireLeader(teamId, principal.getName());

        // 삭제는 메시지 정리 · 캐시 무효화까지 한 트랜잭션으로 묶어야 해서 서비스가 맡는다.
        postJpaService.deleteTeam(teamId);

        // 삭제 후 메인 페이지로 리다이렉트
        return "redirect:/";
    }

    // 팀원 목록 페이지
    @GetMapping("/members/{teamId}")
    public String viewTeamMembers(@PathVariable Long teamId, Model model, Principal principal) {
        // 강퇴 버튼이 달린 관리 화면이다. 명단 자체가 개설자용 정보이므로 조회도 막는다.
        teamAccessService.requireLeader(teamId, principal.getName());

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid team ID: " + teamId));

        // 팀 정보와 팀원 목록을 모델에 추가
        model.addAttribute("team", team);

        return "post/delectAccountPostForm"; // 팀원 목록 페이지로 이동
    }

    // 팀원 강퇴 처리
    @PostMapping("/kickMember")
    public String kickMember(@RequestParam String account_id,
                             @RequestParam String team_id,
                             RedirectAttributes redirectAttributes,
                             Principal principal) {
        TeamMembership leader = teamAccessService.requireLeader(Long.parseLong(team_id), principal.getName());

        // 개설자가 자신을 강퇴하면 리더 없는 팀이 남는다. 그러면 모임 목록에서 사라지고
        // (TeamJpaService.getList가 리더 없는 팀을 건너뛴다) 수정·삭제할 사람도 없어진다.
        // 화면에는 자기 강퇴 버튼이 없지만 요청은 직접 만들 수 있으므로 서버에서 막는다.
        if (String.valueOf(leader.accountId()).equals(account_id)) {
            throw new AccessDeniedException("개설자는 강퇴할 수 없습니다. 모임을 삭제하세요.");
        }

        // 서비스 호출하여 팀원 강퇴
        postTeamService.kickMember(account_id, team_id);

        // 강퇴 후 팀 수정 페이지로 리다이렉트
        redirectAttributes.addFlashAttribute("message", "해당 멤버를 강퇴했습니다.");
        return "redirect:/post/members/" + team_id; // 팀원 목록 페이지로 리다이렉트
    }

}