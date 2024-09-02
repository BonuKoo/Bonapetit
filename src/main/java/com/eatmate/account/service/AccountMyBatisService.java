package com.eatmate.account.service;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.domain.constant.UserRole;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.security.config.AuthConfig;
import com.eatmate.security.dto.AccountContext;
import com.eatmate.security.dto.AuthenticateAccountDto;
import com.eatmate.security.service.FormUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountMyBatisService {

    private final AccountDao accountDao;

    private final FormUserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    public void join(AccountDto dto) {

        // 회원가입
        String password = dto.getPassword();
        dto.setPassword(passwordEncoder.encode(password));
        dto.setRoles(UserRole.USER_ROLE.name());

        int insertedId = accountDao.insertJoin(dto);

        /*
        if (insertedId > 0) {
            // DB에 등록 후 바로 ContextHolder에 등록
            AccountContext userDetails = (AccountContext) userDetailsService.loadUserByUsername(dto.getEmail());

            // 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            // SecurityContextHolder에 인증 객체 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);

        }
    */
    }
}

