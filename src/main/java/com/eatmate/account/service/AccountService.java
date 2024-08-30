package com.eatmate.account.service;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.domain.dto.AccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public boolean updateAccount(AccountDto accountDto) {
        boolean isUpdated = accountDao.updateAccount(accountDto);
        if (isUpdated) {
            System.out.println("카카오 정보가 성공적으로 업데이트되었습니다.");
        } else {
            System.out.println("카카오 정보 업데이트에 실패했습니다.");
        }
        return isUpdated;
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
