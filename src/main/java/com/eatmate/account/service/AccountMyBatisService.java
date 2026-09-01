package com.eatmate.account.service;

import com.eatmate.dao.mybatis.NoticeDao;
import com.eatmate.dao.mybatis.account.AccountDao;
import com.eatmate.dao.mybatis.account.AccountTeamDao;
import com.eatmate.dao.mybatis.chat.ChatMessageDao;
import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.notice.NoticeRepository;
import com.eatmate.domain.constant.UserRole;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.dto.AccountTeamDto;
import com.eatmate.domain.dto.TeamDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountMyBatisService {

    private final AccountDao accountDao;
    private final PasswordEncoder passwordEncoder;
    private final AccountTeamDao accountTeamDao;
    private final ChatMessageDao chatMessageDao;

    private final NoticeDao noticeDao;

    public void join(AccountDto dto) {
        // 회원가입
        String password = dto.getPassword();
        dto.setPassword(passwordEncoder.encode(password));
        dto.setRoles(UserRole.USER_ROLE.name());
        int insertedId = accountDao.insertJoin(dto);
    }

    // 이메일로 사용자 조회
    public AccountDto findByEmail(String email){
        return accountDao.findByEmail(email);
    }

    // OAyth2 ID로 사용자 정보 조회
    public AccountDto findByOauth2Id(String oauth2Id) {
        return accountDao.findByOauth2Id(oauth2Id);
    }

    // 새 계정 생성
    public boolean createAccount(AccountDto accountDto) {
        try {
            // 비밀번호를 인코딩하여 저장
            String encodedPassword = passwordEncoder.encode(accountDto.getPassword());
            accountDto.setPassword(encodedPassword);

            int insertedRows = accountDao.insertAccount(accountDto);
            if (insertedRows > 0) {
                System.out.println("새 계정이 성공적으로 생성되었습니다.");
                return true;
            } else {
                System.out.println("계정 생성에 실패했습니다.");
                return false;
            }
        } catch (DataAccessException e) {
            System.err.println("데이터베이스 오류 발생: " + e.getMessage());
            return false;
        }
    }

    // 로그인 한 회원에 oauth2Id, access_token 업데이트
    public boolean updateAccount(AccountDto accountDto) {
        try {
            int updatedRows = accountDao.updateAccount(accountDto);
            if (updatedRows > 0) {
                System.out.println("성공적으로 업데이트되었습니다.");
                return true;
            } else {
                System.out.println("업데이트에 실패했습니다.");
                return false;
            }
        } catch (DataAccessException e) {
            System.err.println("데이터베이스 오류 발생: " + e.getMessage());
            return false;
        }
    }

    public void updateDetailAccount(AccountDto dto) {
        // 비밀번호를 인코딩하여 업데이트
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(dto.getPassword());
            dto.setPassword(encodedPassword);
        }
        accountDao.updateDetailAccount(dto);
    }

    /**
     * OAuth2 ID를 기반으로 회원 탈퇴 처리.
     *
     * account를 참조하는 테이블을 먼저 정리하지 않으면 FK 제약이 삭제를 막는다.
     * 참조하는 곳은 셋이다 — notice, account_team, chat_message.
     *
     * <p>앞의 둘은 지우고 대화만 남긴다. 탈퇴한 사람의 공지·참여 이력은 그 사람의
     * 것이지만, 대화는 <b>다른 참여자들과 함께 만든 기록</b>이라 한쪽이 나갔다고
     * 사라지면 남은 사람들의 맥락이 끊긴다. 발신자 연결만 끊고 표시 이름은
     * sender_name 스냅샷으로 남는다.
     */
    @Transactional
    public void deleteUserByOauth2Id(String oauth2Id,String accountId) {
        // 대화는 남기고 발신자 연결만 끊는다
        chatMessageDao.detachSenderByAccountId(accountId);
        // Notice 삭제 (Account와 연관된 모든 Notice 삭제)
        noticeDao.deleteByAccountId(accountId);
        // 참여 관계 삭제
        accountTeamDao.deleteByAccountId(accountId);
        // Account 테이블에서 계정 삭제
        accountDao.deleteByOauth2Id(oauth2Id);
    }

    public List<TeamDto> getTeamsForUser(String oauth2Id) {
        AccountDto account = accountDao.findByOauth2Id(oauth2Id);
        if (account == null) {
            System.out.println("해당하는 유저가 없습니다.");
            return Collections.emptyList();  // 유저가 없을 경우 빈 리스트 반환
        }

        // 해당 account_id로 팀 리스트 조회
        return accountTeamDao.findTeamsByAccountId(account.getAccount_id());
    }

    public List<AccountTeamDto> getTeamsWhereIsLeader(String oauth2Id) {
        AccountDto account = accountDao.findByOauth2Id(oauth2Id);
        if (account == null) {
            System.out.println("해당하는 유저가 없습니다.");
            return Collections.emptyList();  // 유저가 없을 경우 빈 리스트 반환
        }

        // 해당 account_id로 팀 리스트 조회
        return accountTeamDao.findTeamsWhereIsLeader(account.getAccount_id());
    }
}

