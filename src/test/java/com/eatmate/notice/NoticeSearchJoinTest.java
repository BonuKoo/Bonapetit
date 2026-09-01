package com.eatmate.notice;

import com.eatmate.dao.repository.notice.impl.NoticeRepository4QueryDslImpl;
import com.eatmate.domain.entity.notice.Notice;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.notice.vo.NoticePageForm;
import com.eatmate.notice.vo.NoticeSearchCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공지 검색이 작성자를 제대로 조인하는지 고정한다.
 *
 * NoticeRepository4QueryDslImpl 은 account.nickname 을 select 하면서 from(notice) 만
 * 두고 join 을 명시하지 않는다. 읽기만 하면 카테시안 곱처럼 보이지만, 실제로 재 보면
 * Hibernate 6 가 FK 기준 inner join 으로 해석한다.
 *
 *   from notice n1_0 join account a1_0 on a1_0.account_id=n1_0.account_id
 *
 * 즉 지금은 정상이다. 다만 조인이 코드에 드러나 있지 않아 QueryDSL/Hibernate 버전이
 * 바뀌면 조용히 달라질 수 있는 자리라, 결과 건수와 작성자 매핑을 여기에 못 박아 둔다.
 */
@DataJpaTest
@ActiveProfiles("test")
class NoticeSearchJoinTest {

    private static final int NOTICES = 3;
    private static final int ACCOUNTS = 4;

    @Autowired private TestEntityManager em;

    /** QueryDslConfig 는 unitName="persistence" 를 요구해 @DataJpaTest 에서 뜨지 않는다. 직접 만든다. */
    private NoticeRepository4QueryDslImpl repository;
    private Account author;

    @BeforeEach
    void setUp() {
        repository = new NoticeRepository4QueryDslImpl(em.getEntityManager());

        author = em.persist(Account.builder()
                .email("author@eatmate.com").nickname("작성자").password("x").build());

        // 공지와 무관한 계정들. 조인이 없으면 이들과도 곱해진다.
        for (int i = 1; i < ACCOUNTS; i++) {
            em.persist(Account.builder()
                    .email("other%d@eatmate.com".formatted(i))
                    .nickname("무관한 계정 " + i).password("x").build());
        }

        Account author2 = em.persist(Account.builder()
                .email("author2@eatmate.com").nickname("작성자2").password("x").build());
        for (int i = 1; i <= NOTICES; i++) {
            em.persist(Notice.builder().title("공지 " + i).content("내용 " + i)
                    .account(i == 2 ? author2 : author).build());
        }
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("계정이 더 많아도 공지 건수만 나오고 작성자가 정확히 매핑된다")
    void searchJoinsAuthorCorrectly() {
        NoticeSearchCondition condition = new NoticeSearchCondition();

        Page<NoticePageForm> page = repository.searchWithPage(condition, PageRequest.of(0, 100));

        System.out.println("### 공지 " + NOTICES + "건 · 계정 " + ACCOUNTS + "개");
        System.out.println("### 검색 결과 행 수 = " + page.getContent().size());
        System.out.println("### total(count 쿼리) = " + page.getTotalElements());
        page.getContent().forEach(r ->
                System.out.println("###   id=" + r.getId() + " title=" + r.getTitle() + " author=" + r.getAuthor()));

        assertThat(page.getContent()).hasSize(NOTICES);
        assertThat(page.getTotalElements()).isEqualTo(NOTICES);
        // 공지 2만 다른 작성자다. 조인이 풀리면 여기가 먼저 깨진다.
        assertThat(page.getContent()).extracting("author")
                .containsExactly("작성자", "작성자2", "작성자");
    }
}
