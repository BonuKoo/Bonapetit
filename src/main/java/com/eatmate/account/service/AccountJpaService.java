package com.eatmate.account.service;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountJpaService {

    private final AccountRepository accountRepository;


}
