package com.eatmate.post.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("post")
public class PostController {

    /**
     *Create
     * TODO
     */
    @GetMapping("create")
    String createPost(){
        return "post/createPostForm";
    }

    /**
     *Read
     * TODO
     */
    @GetMapping("list")
    String getPostList(){
        return "post/listPostForm";
    }

    //TODO
    @GetMapping("detail")
    String getPostDetail(){
        return "post/detailPostForm";
    }

    /**
     *Update
     * TODO
     */
    @GetMapping("update")
    String getPostUpdate(){
        return "post/updatePostForm";
    }

}

