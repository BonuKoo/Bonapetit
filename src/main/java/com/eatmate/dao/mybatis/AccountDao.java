package com.eatmate.dao.mybatis;

import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.entity.user.Account;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.springframework.dao.DataAccessException;

@Mapper
public interface AccountDao {

        @Insert("INSERT INTO account (email, nick_name, password, roles) "
                + "VALUES (#{email}, #{nick_name}, #{password}, #{roles})")
        @Options(useGeneratedKeys = true, keyProperty = "account_id")
        int insertJoin(AccountDto dto) throws DataAccessException;

}
