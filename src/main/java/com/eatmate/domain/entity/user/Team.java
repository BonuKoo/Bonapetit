package com.eatmate.domain.entity.user;

import com.eatmate.domain.entity.post.TeamPost;
import com.eatmate.global.domain.UploadFileOfTeam;
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
public class Team {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "team_name")
    private String teamName;

    //방장
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account_id;

    @OneToMany(mappedBy = "team")
    private List<AccountTeam> members = new ArrayList<>();

    @OneToMany(mappedBy = "team")
    private List<TeamPost> teamPosts = new ArrayList<>();  // 팀과 팀 게시글 간의 관계

    @OneToMany(mappedBy = "team", cascade = CascadeType.MERGE,orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<UploadFileOfTeam> files = new ArrayList<>();

    @Builder
    public Team(Long id, String teamName, Account account_id, List<AccountTeam> members, List<TeamPost> teamPosts, List<UploadFileOfTeam> files) {
        this.id = id;
        this.teamName = teamName;
        this.account_id = account_id;
        this.members = members;
        this.teamPosts = teamPosts;
        this.files = (files != null) ? files : new ArrayList<>();
    }

    /*
            File 연관
         */
    public void addFile(UploadFileOfTeam file) {
        files.add(file);
        file.attachTeam(this);
    }

    public void removeFile(UploadFileOfTeam file) {
        files.remove(file);
        file.attachTeam(null);
    }

}
