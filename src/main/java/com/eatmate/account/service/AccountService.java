package com.eatmate.account.service;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private AccountDao accountDao;
    private AccountRepository accountRepository;

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
