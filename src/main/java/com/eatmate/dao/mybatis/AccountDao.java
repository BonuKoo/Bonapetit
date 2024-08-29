package com.eatmate.dao.mybatis;

import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.entity.user.Account;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.dao.DataAccessException;

@Mapper
public interface AccountDao {

    @Insert("Insert Into account (email, nick_name, password) "
            + "values (#{email}, #{nick_name}, #{password})")
    boolean insertjoin(AccountDto dto) throws DataAccessException;


}
