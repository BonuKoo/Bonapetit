package com.eatmate.dao.mybatis;

import com.eatmate.domain.dto.AccountTeamDto;
import com.eatmate.domain.dto.TeamDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface AccountTeamDao {

    // 가입된 모든 팀 조회
    @Select("SELECT t.team_id, t.team_name, t.description " +
            "FROM TEAM t " +
            "JOIN ACCOUNT_TEAM at ON t.team_id = at.team_id " +
            "WHERE at.account_id = #{account_id}")
    List<TeamDto> findTeamsByAccountId(@Param("account_id") Long account_id);

    @Select("SELECT at.account_team_id, at.account_id, at.team_id, at.is_leader, " +
            "t.team_name, t.description " +
            "FROM account_team at " +
            "JOIN team t ON at.team_id = t.team_id " +
            "WHERE at.account_id = #{account_id} " +
            "AND at.is_leader = TRUE")
    List<AccountTeamDto> findTeamsWhereIsLeader(@Param("account_id") Long account_id);

}
