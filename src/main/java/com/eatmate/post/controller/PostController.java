package com.eatmate.post.controller;

import com.eatmate.domain.entity.post.form.CreatePostForm;
import com.eatmate.post.service.PostJpaService;
import com.eatmate.security.dto.AuthenticateAccountDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;

@Controller
@RequestMapping("post")
public class PostController {

/*    PostJpaService */

    /**
     * Create
     * */
    /*
    @GetMapping("/create")
    String createPost(Model model){

        CreatePostForm form = new CreatePostForm();

        model.addAttribute("createPost", form);

        return "post/createPostForm";
    }

    @PostMapping("/create")
    public String createPost(Principal principal,
                             RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal AuthenticateAccountDto accountDto
    ) throws IOException{

    }
    */

    /**
     *Read
     * TODO
     * */
    /*
    @GetMapping("list")
    String getPostList(){
        return "post/listPostForm";
    }*/

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