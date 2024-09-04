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
import java.util.Optional;

@Controller
@RequestMapping("post")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostJpaService postJpaService;
    private final TeamRepository teamRepository;

    /**
     * Create
     * */

    @GetMapping("/create")
    String createPost(Model model){

        PostForm form = new PostForm();

        model.addAttribute("createPost", form);

        return "post/createPostForm";
    }

    @PostMapping("/createPost")
    public String createPost(RedirectAttributes redirectAttributes,

                             Principal principal,

                             @ModelAttribute("createPost") PostForm postForm
    ) throws IOException{

        postForm.addAuthor(
                principal.getName()
        );

        //게시글 작성 로직
        postJpaService.createChatRoomAndTeamWhenWriteThePost(postForm);
        // 리디렉션 시 사용자에게 메시지를 전달
        redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 생성되었습니다.");
        return "redirect:/post/list";
    }

    /**
     *Read
     * TODO  -> List 로 대충 일단 바꿈 Search며 페이징이며 좀 나중에 하자

    @GetMapping("/search")
    String searchPosts(@RequestParam(value = "searchKeyword", required = false) String searchKeyword,
                       Pageable pageable,
                       Model model){

        TeamSearchCondition condition = new TeamSearchCondition();
        condition.setKeyword(searchKeyword);

        //Page<PostPageDto> result = postJpaService.searchByCondition(condition, pageable);
        //model.addAttribute("posts", result);

        return "post/listPostForm";
    }
     */

    @GetMapping("/list")
    String listPosts(Model model){

        List<Team> all = teamRepository.findAll();

        model.addAttribute("teams",all);

        return "post/listPost";
    }

    @GetMapping("/detail")
    String details(Model model){

        Long id = 1L;

        Team team = teamRepository.findById(id).get();

        model.addAttribute("team",team);

        return "post/detailPostForm";

    }

}