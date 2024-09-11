package com.eatmate.post.service;

import com.eatmate.dao.mybatis.account.AccountTeamDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.beans.Transient;

@Service
@RequiredArgsConstructor
public class PostTeamService {

    private final AccountTeamDao accountTeamDao;

    @Transient
    public void kickMember(Long accountId, Long teamId) {
        accountTeamDao.deleteAccountFromTeam(accountId, teamId);
    }
}
