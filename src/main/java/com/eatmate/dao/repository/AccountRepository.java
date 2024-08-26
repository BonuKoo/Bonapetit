package com.eatmate.dao.repository;

import com.eatmate.domain.entity.user.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account,Long> {
}
