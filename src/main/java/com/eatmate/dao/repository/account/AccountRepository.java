package com.eatmate.dao.repository.account;

import com.eatmate.domain.entity.user.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account,Long> {

    Account findByEmail(String email);

    Account findByOauth2id(String oauth2id);

}
