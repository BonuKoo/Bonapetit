package com.eatmate.account.service;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.domain.constant.UserRole;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.security.dto.AuthenticateAccountDto;
import com.eatmate.security.service.FormUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountMyBatisService {

    private final AccountDao accountDao;

    private final FormUserDetailsService userDetailsService;

    public void join(AccountDto dto){

        // 회원가입

        dto.setRoles(UserRole.USER_ROLE.name());

        int insertedId = accountDao.insertJoin(dto);
        if (insertedId > 0) {
            // 가입에 성공했으므로, 삽입된 DTO의 이메일을 사용
            userDetailsService.loadUserByUsername(dto.getEmail());
        }

    }
}
