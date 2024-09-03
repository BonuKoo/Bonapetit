package com.eatmate.dao.repository.post;

import com.eatmate.post.vo.PostPageDto;
import com.eatmate.post.vo.TeamSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomPostRepository4QueryDsl {

    Page<PostPageDto> searchWithPageConditionIsLocation(TeamSearchCondition condition, Pageable pageable);
    Page<PostPageDto> searchWithPageConditionIsNickname(TeamSearchCondition condition, Pageable pageable);
    Page<PostPageDto> searchWithPageConditionIsTeamName(TeamSearchCondition condition, Pageable pageable);
    Page<PostPageDto> searchWithPageConditionIsTag(TeamSearchCondition condition, Pageable pageable);

}
