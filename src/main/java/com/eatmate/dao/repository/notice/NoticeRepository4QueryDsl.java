package com.eatmate.dao.repository.notice;

import com.eatmate.notice.vo.NoticePageForm;
import com.eatmate.notice.vo.NoticeSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeRepository4QueryDsl {
    Page<NoticePageForm> searchWithPage(NoticeSearchCondition condition, Pageable pageable);
}
