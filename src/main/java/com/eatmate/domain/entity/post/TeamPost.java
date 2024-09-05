package com.eatmate.domain.entity.post;

import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.global.domain.UploadFileOfTeamPost;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class TeamPost {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_post_id")
    private Long id;

    // 작성자
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    // 팀
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    // 조회수
    private int cnt;

    @Builder
    public TeamPost(Long id, Account account, Team team, String title, String content, int cnt, List<UploadFileOfTeamPost> files) {
        this.id = id;
        this.account = account;
        this.team = team;
        this.title = title;
        this.content = content;
        this.cnt = cnt;
        this.files = (files != null) ? files : new ArrayList<>();
    }

    @OneToMany(mappedBy = "teamPost", cascade = CascadeType.MERGE,orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<UploadFileOfTeamPost> files = new ArrayList<>();

     /*
        File 연관
     */

    public void addFile(UploadFileOfTeamPost file) {
        files.add(file);
        file.attachTeamPost(this);
    }

    public void removeFile(UploadFileOfTeamPost file) {
        files.remove(file);
        file.attachTeamPost(null);
    }

}
