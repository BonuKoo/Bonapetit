package com.eatmate.domain.entity.notice;

import com.eatmate.domain.entity.user.Account;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "idx_notice_title", columnList = "title"),
        @Index(name = "idx_notice_account", columnList = "account_id")
})
public class Notice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.MERGE)
    @JoinColumn(name = "account_id")
    private Account account;

    @Builder
    public Notice(Long id, String title, String content, Account account) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.account = account;
    }

    public void updateNotice(String title, String content){
        this.title = title;
        this.content = content;
    }
}
