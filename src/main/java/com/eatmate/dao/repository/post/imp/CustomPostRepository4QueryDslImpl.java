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
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class CustomPostRepository4QueryDslImpl
        //implements CustomPostRepository4QueryDsl
{

    private final JPAQueryFactory queryFactory;

    QAccount account = new QAccount(QAccount.account);
    QPost post = new QPost(QPost.post);
    QTeam team = new QTeam(QTeam.team);


    /*
    @Override
    public Page<PostPageDto> searchWithPage(TeamSearchCondition condition, Pageable pageable) {

        List<PostPageDto> query = queryFactory
                .select(Projections.constructor(PostPageDto.class,
                        post.id,
                        post.title,
                        post.team.teamName,
                        post.createdAt,

                        ))
                .from(post)
                .fetch();
        return null;
    }
*/
    /*
    //검색 조건 : 닉네임
    private BooleanExpression authorEq(String author) {
        if (StringUtils.hasText(author)) {
            return post.account.nickname.containsIgnoreCase(author);
        } else {
            return null;
        }
    }

    //검색 조건 : 팀 이름
    private BooleanExpression teamNameEq(String teamName) {
        if (StringUtils.hasText(teamName)) {
            return team.teamName.containsIgnoreCase(teamName);
        } else {
            return null;
        }
    }*/

    /*
    //날짜 검색 조건
    private BooleanExpression dateEq(LocalDate searchDate){
        return searchDate != null ?
    }
    */

    // 음식 종류 검색 조건
    /*
    private BooleanExpression foodTypeEq(String foodType) {
        return StringUtils.hasText(foodType) ? post.foodType.equalsIgnoreCase(foodType) : null;
    }
    */

}
