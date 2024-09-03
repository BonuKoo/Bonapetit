package com.eatmate.dao.repository.post.imp;

import com.eatmate.dao.repository.post.CustomPostRepository4QueryDsl;

import com.eatmate.domain.entity.post.QPost;
import com.eatmate.domain.entity.user.QAccount;
import com.eatmate.domain.entity.user.QTeam;

import com.eatmate.post.vo.PostPageDto;
import com.eatmate.post.vo.TeamSearchCondition;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class CustomPostRepository4QueryDslImpl implements CustomPostRepository4QueryDsl {

    private final JPAQueryFactory queryFactory;

    QAccount account = QAccount.account;
    QPost post = QPost.post;
    QTeam team = QTeam.team;


    @Override
    public Page<PostPageDto> searchWithPageConditionIsLocation(TeamSearchCondition condition, Pageable pageable) {
        return searchWithPage(condition, pageable, locationEq(condition.getLocation()));
    }

    @Override
    public Page<PostPageDto> searchWithPageConditionIsNickname(TeamSearchCondition condition, Pageable pageable) {
        return searchWithPage(condition, pageable, authorEq(condition.getAuthor()));
    }

    @Override
    public Page<PostPageDto> searchWithPageConditionIsTeamName(TeamSearchCondition condition, Pageable pageable) {
        return searchWithPage(condition, pageable, teamNameEq(condition.getTeamName()));
    }

    @Override
    public Page<PostPageDto> searchWithPageConditionIsTag(TeamSearchCondition condition, Pageable pageable) {
        return searchWithPage(condition, pageable, tagEq(condition.getTag()));
    }

    private Page<PostPageDto> searchWithPage(TeamSearchCondition condition, Pageable pageable, BooleanExpression predicate) {
        List<PostPageDto> query = queryFactory
                .select(Projections.constructor(PostPageDto.class,
                        post.id,
                        post.title,
                        post.team.teamName,
                        post.account.nickname,
                        post.createdAt
                ))
                .from(post)
                .where(predicate)
                .groupBy(post.id)
                .orderBy(post.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(post.count())
                .from(post)
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(query, pageable, total);
    }

    // 검색 조건 : 지역
    private BooleanExpression locationEq(String location) {
        return StringUtils.hasText(location) ? post.location.containsIgnoreCase(location) : null;
    }

    // 검색 조건 : 작성자 닉네임
    private BooleanExpression authorEq(String author) {
        return StringUtils.hasText(author) ? post.account.nickname.containsIgnoreCase(author) : null;
    }

    // 검색 조건 : 팀 이름
    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? post.team.teamName.containsIgnoreCase(teamName) : null;
    }


    // 검색 조건 : 관련 태그 TODO
    private BooleanExpression tagEq(String tag) {
        return StringUtils.hasText(tag) ? post.tags.any().tagName.containsIgnoreCase(tag) : null;
    }
}
