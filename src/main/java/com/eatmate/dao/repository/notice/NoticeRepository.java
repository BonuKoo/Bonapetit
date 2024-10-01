package com.eatmate.dao.repository.notice;

import com.eatmate.domain.entity.notice.Notice;
import com.eatmate.notice.vo.NoticePageForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long>,NoticeRepository4QueryDsl {
    // 제목으로 검색하는 쿼리
    Page<Notice> findByTitleContaining(String title, Pageable pageable);


    Page<Notice> findByTitleContainingOrderByIdAsc(String title, Pageable pageable);


    @Query("SELECT n FROM Notice n JOIN FETCH n.account a WHERE n.title LIKE %:title% ORDER BY n.id ASC")
    Page<Notice> findNoticesWithFetchJoinAccountByTitleContaining(@Param("title") String title, Pageable pageable);


    @Query("SELECT new com.eatmate.notice.vo.NoticePageForm(n.id, n.title, a.nickname, n.content) " +
            "FROM Notice n " +
            "JOIN n.account a " +
            "WHERE n.title LIKE %:title%")
    Page<NoticePageForm> findNoticePageFormByTitleContaining(@Param("title") String title, Pageable pageable);

}
