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
    public void kickMember(String account_id, String team_id) {
        // 1. account_team 테이블에서 해당 유저가 속한 팀과의 관계 삭제
        accountTeamDao.deleteAccountFromTeam(account_id, team_id);
    }
}
