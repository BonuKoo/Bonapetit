package com.eatmate.domain.entity.user;

import com.eatmate.domain.entity.Tag;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.map.KakaoMap;
import com.eatmate.domain.entity.post.TeamPost;
import com.eatmate.domain.global.BaseTimeEntity;
import com.eatmate.global.domain.UploadFileOfTeam;
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
public class Team extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "team_name")
    private String teamName;

    //리더 컬럼

    // 리더를 위한 OneToOne 관계 설정
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")  // 외래 키를 'leader_id'로 명시
    private Account leader;

    @Column(name = "description")
    private String description;

    // Team과 KakaoMap의 다대일 관계 매칭
    @ManyToOne
    @JoinColumn(name = "map_id")
    private KakaoMap map;

    @OneToMany(mappedBy = "team",cascade = CascadeType.PERSIST,orphanRemoval = true)
    private List<AccountTeam> members = new ArrayList<>();

    //채팅방과 팀을 일대일로 매핑
    @OneToOne(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ChatRoom chatRoom;

    //리뷰 속성
    @OneToMany(mappedBy = "team")
    private List<TeamPost> teamPosts = new ArrayList<>();  // 팀과 팀 게시글 간의 관계
    
    //태그
    @OneToMany(mappedBy = "team", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.PERSIST,orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<UploadFileOfTeam> files = new ArrayList<>();

    @Builder
    public Team(Long id, String teamName, Account leader, String description, KakaoMap map, List<AccountTeam> members, List<TeamPost> teamPosts, List<UploadFileOfTeam> files) {
        this.id = id;
        this.teamName = teamName;
        this.leader = leader;
        this.description = description;
        this.map = map;
        this.members = members != null ? members : new ArrayList<>();
        this.files = (files != null) ? files : new ArrayList<>();
        this.teamPosts = teamPosts;

    }

    /**
     AccountTeam
     */
    public void addAccountTeam(AccountTeam accountTeam){
        this.members.add(accountTeam);
        accountTeam.updateTeam(this);
    }

    /**
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

    /**
     * 멤버 수 반환
     */

    @Transient
    public int getMembersCount() {
        return this.members.size();
    }


    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
        if (chatRoom != null && chatRoom.getTeam() != this) {
            chatRoom.setTeam(this); // 이미 관계가 설정되어 있지 않다면 설정
        }
    }

}
