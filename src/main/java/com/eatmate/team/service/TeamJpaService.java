package com.eatmate.team.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.team.AccountTeamRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.AccountTeam;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.team.vo.TeamForm;
import com.eatmate.team.vo.TeamVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.ldap.PagedResultsControl;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamJpaService {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter CREATED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final AccountTeamRepository accountTeamRepository;
    /*
    @Transactional
    public TeamForm joinTeam(TeamForm teamForm){

        Account account = accountRepository.findByOauth2id(teamForm.getUserName());

        Team team = teamRepository.findById(teamForm.getTeamId()).get();

        AccountTeam accountTeam = AccountTeam.builder()
                .account(account)
                //.team(team)
                .isLeader(false)
                .build();

        team.addAccountTeam(accountTeam);

        Team savedTeam = teamRepository.save(team);

        //RoomId, 공통 닉네임, 팀 이름을 저장 후 반환
        teamForm.attachRoomIdAndNickname(
                savedTeam.getChatRoom().getRoomId(),
                account.getNickname(),
                savedTeam.getTeamName());

        return teamForm;

    }
    */

    @Transactional
    public TeamForm joinTeam(TeamForm teamForm){

        Account account = accountRepository.findByOauth2id(teamForm.getUserName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다: " + teamForm.getUserName()));
        Team team = teamRepository.findById(teamForm.getTeamId()).orElseThrow(() -> new IllegalArgumentException("Invalid team ID"));

        // 사용자가 이미 팀에 속해있는지 확인
        Optional<AccountTeam> existingAccountTeam = accountTeamRepository.findByAccountAndTeam(account, team);

        if (existingAccountTeam.isPresent()) {
            // 이미 팀에 가입되어 있으면 바로 팀 정보를 반환
            teamForm.attachRoomIdAndNickname(
                    team.getChatRoom().getRoomId(),
                    account.getNickname(),
                    team.getTeamName());
            return teamForm;
        }

        // 가입되어 있지 않으면 새로운 AccountTeam 엔티티를 생성
        AccountTeam accountTeam = AccountTeam.builder()
                .account(account)
                .team(team)
                .isLeader(false)
                .build();

        team.addAccountTeam(accountTeam);
        teamRepository.save(team);

        teamForm.attachRoomIdAndNickname(
                team.getChatRoom().getRoomId(),
                account.getNickname(),
                team.getTeamName());

        return teamForm;
    }

    /**
     * 모임 목록.
     *
     * 저장소가 프로젝션으로 한 번에 채워 준다. 예전에는 Team 엔티티를 받아 항목마다
     * 개설자를 다시 조회하고 인원수를 세려고 참여자를 통째로 로드했다. 10건 페이지에
     * 쿼리가 42건 나갔고, 그중 40건이 항목마다 반복되는 것이었다.
     *
     * 남는 일은 표시 형식 변환뿐이다. 날짜 포맷은 화면의 관심사라 저장소에 두지 않는다.
     */
    public Page<TeamVo> getList(int page, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE);

        return teamRepository.findListPageByKeyword(keyword, pageable)
                .map(row -> TeamVo.builder()
                        .teamId(row.teamId())
                        .teamName(row.teamName())
                        .addressName(row.addressName())
                        .roadAddressName(row.roadAddressName())
                        .placeName(row.placeName())
                        .author(row.author())
                        .memCnt(row.memberCount())
                        .createdDate(row.createdAt().format(CREATED_DATE_FORMAT))
                        .build());
    }

}
