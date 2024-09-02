package com.eatmate.domain.entity.post;

import com.eatmate.domain.entity.Tag;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.domain.global.BaseTimeEntity;
import com.eatmate.global.domain.UploadFileOfPost;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Post extends BaseTimeEntity {

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

    @OneToOne
    private Team team;

    // Tag와의 일대다 관계 설정
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();

    // File과의 일대다 관계 설정
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UploadFileOfPost> files = new ArrayList<>();

    @Builder
    public Post(Long id, Account account, Team team, String title, String content, List<Tag> tags, List<UploadFileOfPost> files) {
        this.id = id;
        this.account = account;
        this.team = team;
        this.title = title;
        this.content = content;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.files = files != null ? files : new ArrayList<>();
    }

    /*
        Tag 연관 관리 메서드
     */
    public void addTag(Tag tag) {
        tags.add(tag);
        tag.attachPost(this);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.attachPost(null);
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
