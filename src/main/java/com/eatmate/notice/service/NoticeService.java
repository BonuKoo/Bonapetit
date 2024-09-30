package com.eatmate.notice.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.notice.NoticeRepository;
import com.eatmate.domain.entity.notice.Notice;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.notice.vo.NoticeForm;
import com.eatmate.notice.vo.NoticePageForm;
import com.eatmate.notice.vo.NoticeSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public Page<Notice> findAllWithPageV1(Pageable pageable){

        List<Notice> allNotices = noticeRepository.findAll();
        int start = (int) pageable.getOffset();

        int end = Math.min((start + pageable.getPageSize()), allNotices.size());

        List<Notice> paginatedNotices = allNotices.subList(start, end);

        // Page 객체로 반환
        return new PageImpl<>(paginatedNotices, pageable, allNotices.size());

    }

    public Page<Notice> findAllWithPageV2(String titleCondition, Pageable pageable) {
        // where 조건이 있을 때와 없을 때를 구분하여 쿼리 실행
        if (titleCondition == null || titleCondition.isEmpty()) {
            return noticeRepository.findAll(pageable); // 조건 없이 모든 데이터 페이징
        } else {
            return noticeRepository.findByTitleContaining(titleCondition, pageable); // 조건에 따른 페이징
        }
    }

    public List<Notice> findAllWithPageV3(String title, int page, int size){
        Pageable pageable = PageRequest.of(page, size);

        Page<Notice> pageResult;

        // title이 null이거나 빈 문자열일 경우 전체 Notice 조회
        if (title == null || title.isEmpty()) {
            pageResult = noticeRepository.findAll(pageable);
        } else {
            pageResult = noticeRepository.findByTitleContainingOrderByIdAsc(title, pageable);
        }

        // Page 객체에서 List로 변환하여 반환
        return pageResult.getContent();
    }


    public List<Notice> findAllWithPageV4(String title, int page, int size) {
        // page 값을 1 기반에서 0 기반으로 변환
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Notice> pageOfBoards = noticeRepository.findNoticesWithFetchJoinAccountByTitleContaining(title, pageable);
        return pageOfBoards.getContent();

    }

    public Page<NoticePageForm> findNoticeByTitleInDtoV5(String title, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);  // 1-based paging to 0-based
        return noticeRepository.findNoticePageFormByTitleContaining(title, pageable);
    }

}
