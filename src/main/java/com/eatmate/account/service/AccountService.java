package com.eatmate.account.service;

import com.eatmate.dao.mybatis.account.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountDao accountDao;
    private final AccountRepository accountRepository;

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