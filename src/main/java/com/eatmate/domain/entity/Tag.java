package com.eatmate.domain.entity;

import com.eatmate.domain.entity.post.Post;
import jakarta.persistence.*;

@Entity
public class Tag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tagName;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    public void attachPost(Post post) {
        this.post = post;
    }

}
