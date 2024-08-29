package com.eatmate.account.service;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
