package com.eatmate.global.domain;

import jakarta.persistence.*;

/*
@Data
@NoArgsConstructor
@Table(name = "UploadFile")
@Entity
*/

public class UploadFileOfEntity {


    /**
     * TODO :: 싹 다 Common 속성으로 빼둬야 한다
     */


    /*
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
    @JoinColumn(name = "entity_id")
    private Entity entity;          // 해당 파일이 속한 엔티티

    // 파일 이름과 저장된 파일 이름을 사용하는 생성자
    public UploadFileOfEntity(String uploadFileName, String storeFileName, String filePath, String fileType, long fileSize) {
        this.uploadFileName = uploadFileName;
        this.storeFileName = storeFileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

    // 게시물과의 연관 관계 설정 메서드
    public void attachEntity(Entity entity){
        this.entity = entity;
    }
    */

}