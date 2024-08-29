package com.eatmate.dao.repository.account;

import com.eatmate.domain.entity.user.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account,Long> {
}
