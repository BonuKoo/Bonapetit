package com.eatmate.dao.repository.post;

import com.eatmate.post.vo.PostPageDto;
import com.eatmate.post.vo.TeamSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomPostRepository4QueryDsl {

    Page<PostPageDto> searchWithPage(TeamSearchCondition condition, Pageable pageable);

}
