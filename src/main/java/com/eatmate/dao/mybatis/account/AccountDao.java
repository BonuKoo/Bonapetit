package com.eatmate.dao.mybatis.account;

import com.eatmate.domain.dto.AccountDto;
import org.apache.ibatis.annotations.*;
import org.springframework.dao.DataAccessException;

@Mapper
public interface AccountDao {

        @Insert("INSERT INTO Account (email, nick_name, password, roles) "
                + "VALUES (#{email}, #{nick_name}, #{password}, #{roles})")
        @Options(useGeneratedKeys = true, keyProperty = "account_id")
        int insertJoin(AccountDto dto) throws DataAccessException;

        // 카카오 새 계정 삽입
        @Insert("INSERT INTO Account (email, nick_name, password, roles, oauth2_id, access_token, provider) " +
                "VALUES (#{email}, #{nick_name}, #{password}, #{roles}, #{oauth2_id}, #{access_token}, #{provider})")
        int insertAccount(AccountDto dto);

        @Select("SELECT * FROM Account WHERE email = #{email}")
        AccountDto findByEmail(@Param("email") String email);

        @Select("SELECT  account_id, email, nick_name, password, roles, oauth2_id, access_token, provider FROM Account WHERE oauth2_id = #{oauth2_id}")
        AccountDto findByOauth2Id(@Param("oauth2_id") String oauth2Id);

        @Update("UPDATE Account SET oauth2_id = #{oauth2_id}, access_token = #{access_token} WHERE email = #{email}")
        int updateAccount(AccountDto dto);

        @Select("select email, nick_name, password from Account WHERE oauth2_id = #{oauth2_id}")
        AccountDto selectUser(@Param("oauth2_id") int oauth2Id);

        @Update("UPDATE Account SET nick_name = #{nick_name} WHERE oauth2_id = #{oauth2_id}")
        void updateDetailAccount(AccountDto dto);

        // 회원 탈퇴를 위한 쿼리 추가
        @Delete("DELETE FROM Account WHERE oauth2_id = #{oauth2_id}")
        void deleteByOauth2Id(@Param("oauth2_id") String oauth2Id);
}
