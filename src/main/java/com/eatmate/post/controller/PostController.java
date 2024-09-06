package com.eatmate.post.controller;

import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.post.vo.PostForm;
import com.eatmate.post.service.PostJpaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final TeamRepository teamRepository;

    // Create Post 페이지
    @GetMapping("/create")
    String createPost(Model model){
        PostForm form = new PostForm();
        model.addAttribute("createPost", form);
        return "post/createPostForm";
    }

    // 게시글 및 팀 생성
    @PostMapping("/createPost")
    public String createPost(RedirectAttributes redirectAttributes, Principal principal, @ModelAttribute("createPost") PostForm postForm) throws IOException{
        postForm.addAuthor(principal.getName());
        postJpaService.createChatRoomAndTeamWhenWriteThePost(postForm);
        redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 생성되었습니다.");
        return "redirect:/post/list";
    }

    // Team 목록 조회
    @GetMapping("/list")
    String listPosts(Model model){
        List<Team> all = teamRepository.findAll();
        model.addAttribute("teams", all);
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
}