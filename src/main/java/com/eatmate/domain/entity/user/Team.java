package com.eatmate.domain.entity.user;

import com.eatmate.domain.entity.Tag;
import com.eatmate.domain.entity.chat.ChatRoom;
import com.eatmate.domain.entity.post.TeamPost;
import com.eatmate.domain.global.BaseTimeEntity;
import com.eatmate.global.domain.UploadFileOfTeam;
import com.eatmate.map.vo.MapVo;
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

    @Column(name = "description")
    private String description;

    @Column(name = "map_id")
    private String mapId;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "place_name")
    private String placeName;

    @Column(name = "place_url")
    private String placeUrl;

    @Column(name = "road_address_name")
    private String roadAddressName;

    @Column(name = "x")
    private String x;

    @Column(name = "y")
    private String y;

    @OneToMany(mappedBy = "team",cascade = CascadeType.ALL,orphanRemoval = true)
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
    public Team(Long id,
                String teamName,
                String description,
                String mapId,
                String addressName,
                String phone,
                String placeName,
                String placeUrl,
                String roadAddressName,
                String x,
                String y,
                List<AccountTeam> members,
                List<TeamPost> teamPosts,
                List<UploadFileOfTeam> files) {
        this.id = id;
        this.teamName = teamName;
        this.description = description;
        this.mapId = mapId;
        this.addressName = addressName;
        this.phone = phone;
        this.placeName = placeName;
        this.placeUrl = placeUrl;
        this.roadAddressName = roadAddressName;
        this.x = x;
        this.y = y;
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

    // 팀 수정 메서드
    public void updateTeam(String teamName, String description, MapVo mapVo) {
        this.teamName = teamName;
        this.description = description;
        this.mapId = mapVo.getMapId();
        this.addressName = mapVo.getAddressName();
        this.phone = mapVo.getPhone();
        this.placeName = mapVo.getPlaceName();
        this.placeUrl = mapVo.getPlaceUrl();
        this.roadAddressName = mapVo.getRoadAddressName();
        this.x = mapVo.getX();
        this.y = mapVo.getY();
    }

    // 팀 이름 수정 메서드
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    // 설명 수정 메서드
    public void setDescription(String description) {
        this.description = description;
    }

}
