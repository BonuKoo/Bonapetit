package com.eatmate.dao.mybatis;

import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.entity.user.Account;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.dao.DataAccessException;
import org.springframework.data.repository.query.Param;

@Mapper
public interface AccountDao {

    @Insert("Insert Into account (email, nick_name, password, oauth2_id, access_token) "
            + "values (#{email}, #{nick_name}, #{password}, null, null)")
    boolean insertjoin(AccountDto dto) throws DataAccessException;

    @Select("SELECT * FROM account WHERE oauth2_id = #{oauth2_id}")
    AccountDto findByOauth2Id(@Param("oauth2_id") String oauth2Id);

    @Update("UPDATE account SET access_token = #{access_token} WHERE oauth2_id = #{oauth2_id}")
    boolean updateAccount(AccountDto dto);

    @Select("SELECT * FROM account WHERE email = #{email}")
    AccountDto findByEmail(@Param("email") String email);
}
