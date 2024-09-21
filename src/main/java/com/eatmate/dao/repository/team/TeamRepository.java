package com.eatmate.dao.repository.team;

import com.eatmate.domain.entity.user.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team,Long>
{
    @Query("select t " +
            "from Team t " +
            "where " +
            "t.teamName like %:keyword% or " +
            "t.placeName like %:keyword% or " +
            "t.addressName like %:keyword% or " +
            "t.roadAddressName like %:keyword% " +
            "order by " +
            "t.createdAt desc")
    Page<Team> findPageByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
