package com.eatmate.initializer;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.notice.NoticeRepository;
import com.eatmate.domain.entity.notice.Notice;
import com.eatmate.domain.entity.user.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

//@Component
public class DataInitializer
//        implements CommandLineRunner
{

    private final NoticeRepository noticeRepository;
    private final AccountRepository accountRepository;

    public DataInitializer(NoticeRepository noticeRepository, AccountRepository accountRepository) {
        this.noticeRepository = noticeRepository;
        this.accountRepository = accountRepository;
    }
    /*
    @Override
    public void run(String... args) throws Exception {

        List<Notice> notices = new ArrayList<>();

        Account account = accountRepository.findById(1L).orElseThrow();

        // 6만 건의 데이터를 생성하여 리스트에 추가
        for (int i = 1; i <= 60000; i++) {
            Notice notice = Notice.builder()
                    .title("Title " + i)
                    .content("Content " + i)
                    .account(account)
                    .build();
            notices.add(notice);

            // 1000건씩 저장 (Batch 처리)
            if (i % 1000 == 0) {
                noticeRepository.saveAll(notices);
                notices.clear();  // 리스트 비우기
            }
        }

        // 남은 데이터 저장
        if (!notices.isEmpty()) {
            noticeRepository.saveAll(notices);
        }

        System.out.println("6만 건의 데이터가 성공적으로 저장되었습니다.");
    }*/
}
