package com.eatmate.account.service;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.domain.dto.AccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private AccountDao accountDao;
    private AccountRepository accountRepository;

    // 이메일로 사용자 조회
    public AccountDto findByEmail(String email){
        return accountDao.findByEmail(email);
    }

    // OAyth2 ID로 사용자 정보 조회
    public AccountDto findByOauth2Id(String oauth2Id) {
        return accountDao.findByOauth2Id(oauth2Id);
    }

    // 로그인 한 회원에 oauth2Id, access_token 업데이트
    public void updateAccount(AccountDto accountDto) {
        accountDao.updateAccount(accountDto);
    }

    /*
    @Transactional
    void createAccountMyBatis(){
        accountDao.join();
    }

    @Transactional
    void createAccountRepository(){
        accountRepository.save();
    }
    */

}
