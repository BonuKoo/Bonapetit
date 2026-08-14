package com.eatmate.dao.repository.account;

import com.eatmate.domain.entity.user.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {

    Account findByEmail(String email);

    Optional<Account> findByOauth2id(String oauth2id);

}
