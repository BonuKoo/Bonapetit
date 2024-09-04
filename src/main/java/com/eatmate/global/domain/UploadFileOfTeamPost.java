package com.eatmate.global.domain;

import com.eatmate.domain.entity.post.TeamPost;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Table(name = "UploadFile_TeamPost")
@Entity
public class UploadFileOfTeamPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uploadFileName; // 고객이 업로드한 파일명
    private String storeFileName;  // 서버 내부에서 관리하는 파일명 -> uuid

    @Column(nullable = false)
    private String filePath;      // 파일 저장 경로

    @Column(nullable = false)
    private String fileType;      // 파일 타입 (MIME type)

    @Column(nullable = false)
    private long fileSize;        // 파일 크기 (바이트 단위)

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "teamPost_id")
    private TeamPost teamPost;          // 해당 파일이 속한 엔티티

    // 파일 이름과 저장된 파일 이름을 사용하는 생성자
    public UploadFileOfTeamPost(String uploadFileName, String storeFileName, String filePath, String fileType, long fileSize) {
        this.uploadFileName = uploadFileName;
        this.storeFileName = storeFileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

    // 게시물과의 연관 관계 설정 메서드
    public void attachTeamPost(TeamPost teamPost){
        this.teamPost = teamPost;
    }
}