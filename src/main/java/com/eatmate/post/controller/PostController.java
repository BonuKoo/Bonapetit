package com.eatmate.post.controller;

import com.eatmate.post.vo.PostForm;
import com.eatmate.post.service.PostJpaService;
import com.eatmate.post.vo.PostPageDto;
import com.eatmate.post.vo.TeamSearchCondition;
import com.eatmate.security.dto.AuthenticateAccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.awt.print.Pageable;
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
        return "redirect:/post/list";
    }

    /**
     *Read
     * TODO
     * */
    /*
    @GetMapping("/search")
    String searchPosts(@RequestParam("searchType") String searchType,
                       @RequestParam(value = "searchKeyword", required = false) String searchKeyword,
                       @RequestParam(value = "searchDate", required = false) LocalDate searchDate,
                       Pageable pageable,
                       Model model){

        TeamSearchCondition condition = new TeamSearchCondition();

        switch (searchType) {
            case "location":
                condition.setLocation(searchKeyword);
                break;
            case "title":
                condition.setTeamName(searchKeyword);
                break;
            case "date":
                condition.setSearchDate(searchDate);
                break;
            case "author":
                condition.setAuthor(searchKeyword);
                break;
            case "teamName":
                condition.setTeamName(searchKeyword);
                break;
            case "foodKind":
                condition.setFoodKind(searchKeyword);
                break;
            default:
                throw new IllegalArgumentException("잘못된 검색 유형입니다.");
        }

        Page<PostPageDto> result = postJpaService.searchByCondition(condition,pageable);
        model.addAttribute("posts", result);
        return "post/listPostForm";
    }
    */

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