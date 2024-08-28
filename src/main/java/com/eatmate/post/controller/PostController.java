package com.eatmate.post.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("post")
public class PostController {

    @GetMapping("create")
    String createPost(){
        return "post/createPostForm";
    }

    @GetMapping("list")
    String getPostList(){
        return "post/listPostForm";
    }


}
