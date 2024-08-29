package com.eatmate.domain.entity.post;

import com.eatmate.domain.entity.user.Account;
import com.eatmate.global.domain.UploadFileOfPost;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;
    
    //작성자
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;
    
    //조회수

    @Column(name="cnt")
    private int cnt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.MERGE,orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<UploadFileOfPost> files = new ArrayList<>();

    @Builder
    public Post(Long id, Account account, String title, String content, int cnt, List<UploadFileOfPost> files) {
        this.id = id;
        this.account = account;
        this.title = title;
        this.content = content;
        this.cnt = cnt;
        this.files = (files != null) ? files : new ArrayList<>();
    }

    /*
        File 연관
     */

    public void addFile(UploadFileOfPost file) {
        files.add(file);
        file.attachPost(this);
    }

    public void removeFile(UploadFileOfPost file) {
        files.remove(file);
        file.attachPost(null);
    }

}
