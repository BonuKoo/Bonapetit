package com.eatmate.dao.mybatis;

import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.entity.user.Account;
import org.apache.ibatis.annotations.*;
import org.springframework.dao.DataAccessException;

@Mapper
public interface AccountDao {

        @Insert("INSERT INTO account (email, nick_name, password, roles) "
                + "VALUES (#{email}, #{nick_name}, #{password}, #{roles})")
        @Options(useGeneratedKeys = true, keyProperty = "account_id")
        int insertJoin(AccountDto dto) throws DataAccessException;

        @Select("SELECT * FROM account WHERE oauth2_id = #{oauth2_id}")
        AccountDto findByOauth2Id(@Param("oauth2_id") String oauth2Id);

        @Update("UPDATE account SET oauth2_id = #{oauth2_id}, access_token = #{access_token} WHERE email = #{email}")
        int updateAccount(AccountDto dto);

        @Select("SELECT * FROM account WHERE email = #{email}")
        AccountDto findByEmail(@Param("email") String email);

}
