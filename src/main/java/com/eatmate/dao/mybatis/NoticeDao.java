package com.eatmate.dao.mybatis;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

@Mapper
public interface NoticeDao {

    // 특정 계정의 모든 Notice 삭제
    @Delete("DELETE FROM Notice WHERE account_id = #{account_id}")
    void deleteByAccountId(@Param("account_id") String account_id);
}
