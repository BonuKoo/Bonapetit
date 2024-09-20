package com.eatmate.dao.repository.notice;

import com.eatmate.domain.entity.notice.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long>,NoticeRepository4QueryDsl {
}
