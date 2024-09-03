package com.eatmate.post.controller;

import com.eatmate.post.vo.PostForm;
import com.eatmate.post.service.PostJpaService;
import com.eatmate.post.vo.PostPageDto;
import com.eatmate.post.vo.TeamSearchCondition;
import com.eatmate.security.dto.AuthenticateAccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.io.IOException;
import java.time.LocalDate;

@Controller
@RequestMapping("post")
@RequiredArgsConstructor
public class PostController {

    private final PostJpaService postJpaService;

/*    PostJpaService */

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
                             @AuthenticationPrincipal AuthenticateAccountDto accountDto,
                             @ModelAttribute("createPost") PostForm postForm
    ) throws IOException{

        postForm.addAuthor(accountDto.getEmail());

        //게시글 작성 로직
        postJpaService.createChatRoomAndTeamWhenWriteThePost(postForm);

        // 리디렉션 시 사용자에게 메시지를 전달
        redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 생성되었습니다.");
        return "redirect:/post/search";
    }

    /**
     *Read
     * TODO
     */

    @GetMapping("/search")
    String searchPosts(@RequestParam(value = "searchType", required = false) String searchType,
                       @RequestParam(value = "searchKeyword", required = false) String searchKeyword,
                       Pageable pageable,
                       Model model){

        TeamSearchCondition condition = new TeamSearchCondition();

        if (StringUtils.hasText(searchKeyword)) {
            switch (searchType) {
                case "location":
                    condition.setLocation(searchKeyword);
                    break;
                case "author":
                    condition.setAuthor(searchKeyword);
                    break;
                case "teamName":
                    condition.setTeamName(searchKeyword);
                    break;
                /** 미 구현
                 case "tag":
                 condition.setTag(searchKeyword);
                 break;
                 */
                default:
                    throw new IllegalArgumentException("잘못된 검색 유형입니다.");
            }
        } else {
            // 모든 조건이 비어 있으면 location을 "서울 종로구"로 설정
            condition.setLocation("서울 종로구");
        }

        Page<PostPageDto> result = postJpaService.searchByCondition(condition, pageable);
        model.addAttribute("posts", result);
        return "post/listPostForm";
    }


    //TODO
    /*
    @GetMapping("detail")
    String getPostDetail(){
        return "post/detailPostForm";
    }*/

    /**
     *Update
     * TODO
     */
/*
    @GetMapping("update")
    String getPostUpdate(){
        return "post/updatePostForm";
    }
*/
}