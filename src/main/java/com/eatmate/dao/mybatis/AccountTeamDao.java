package com.eatmate.dao.mybatis;

import com.eatmate.domain.dto.TeamDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface AccountTeamDao {

    @Select("SELECT t.team_id, t.team_name, t.description " +
            "FROM TEAM t " +
            "JOIN ACCOUNT_TEAM at ON t.team_id = at.team_id " +
            "WHERE at.account_id = #{account_id}")
    List<TeamDto> findTeamsByAccountId(@Param("account_id") Long account_id);

}
