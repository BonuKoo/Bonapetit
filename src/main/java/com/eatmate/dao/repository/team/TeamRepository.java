package com.eatmate.dao.repository.team;

import com.eatmate.domain.entity.user.Team;
import com.eatmate.team.vo.TeamListRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team,Long>
{
    /**
     * 모임 목록 화면용 프로젝션.
     *
     * <p>엔티티 대신 필요한 값만 뽑는다. Team을 돌려주면 개설자를 찾으려 페이지 항목마다
     * 조회가 붙고, 인원수를 세려고 members 컬렉션이 통째로 로드되며, AccountTeam의
     * @ManyToOne이 EAGER라 연관까지 따라온다. 10건 페이지에 쿼리 42건이 나갔다.
     *
     * <p>개설자는 <b>inner join</b>이다. 기존 코드도 개설자가 없는 모임은 목록에서
     * 건너뛰었으므로 보이는 결과는 같다. 다만 그때는 건너뛴 항목이 전체 건수에는
     * 그대로 남아 페이지에 빈자리가 생겼는데, count 쿼리에도 같은 조인을 걸어
     * 전체 건수와 실제 항목 수가 어긋나지 않게 했다.
     *
     * <p>인원수의 size()는 상관 서브쿼리로 번역되어 같은 쿼리 안에서 계산된다.
     */
    @Query(value = "select new com.eatmate.team.vo.TeamListRow(" +
            "t.id, t.teamName, t.addressName, t.roadAddressName, t.placeName, " +
            "leader.account.nickname, size(t.members), t.createdAt) " +
            "from Team t " +
            "join AccountTeam leader on leader.team = t and leader.isLeader = true " +
            "where " +
            "t.teamName like %:keyword% or " +
            "t.placeName like %:keyword% or " +
            "t.addressName like %:keyword% or " +
            "t.roadAddressName like %:keyword% " +
            "order by " +
            "t.createdAt desc",
            countQuery = "select count(t) " +
                    "from Team t " +
                    "join AccountTeam leader on leader.team = t and leader.isLeader = true " +
                    "where " +
                    "t.teamName like %:keyword% or " +
                    "t.placeName like %:keyword% or " +
                    "t.addressName like %:keyword% or " +
                    "t.roadAddressName like %:keyword%")
    Page<TeamListRow> findListPageByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
