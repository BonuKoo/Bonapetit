package com.eatmate.notice.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.notice.NoticeRepository;
import com.eatmate.domain.entity.notice.Notice;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.notice.vo.NoticeForm;
import com.eatmate.notice.vo.NoticePageForm;
import com.eatmate.notice.vo.NoticeSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class NoticeService {
    private final AccountRepository accountRepository;
    private final NoticeRepository noticeRepository;
    
    //생성
    @Transactional
    public void createNotice(NoticeForm noticeForm){
        Account account = accountRepository.findByOauth2id(noticeForm.getAuthor());

        Notice notice = Notice.builder()
                .title(noticeForm.getTitle())
                .content(noticeForm.getContent())
                .account(account)
                .build();

        noticeRepository.save(notice);
    }

    //조회
    public NoticeForm findDetailById(Long noticeId){
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow();
        return
        NoticeForm.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .author(notice.getAccount().getNickname())
                .build();
    }

    //@Cacheable(value = "noticeCache", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #sortProperty + '-' + #sortDirection")
    public Page<NoticePageForm> searchWithPage(NoticeSearchCondition condition, Pageable pageable){
        return noticeRepository.searchWithPage(condition,pageable);
    }
    //수정
    @Transactional
    public void updateNotice(Long noticeId, String title,String content){
        Notice notice = noticeRepository.findById(noticeId).orElse(null);
        if (notice != null){
            notice.updateNotice(title, content);
            noticeRepository.save(notice);
        }
    }
    //삭제
    public void removeNotice(Long id){
        Notice notice = noticeRepository.findById(id)
                .orElseThrow();
        noticeRepository.delete(notice);
    }
}
