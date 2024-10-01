package com.eatmate.notice.controller;

import com.eatmate.domain.entity.notice.Notice;
import com.eatmate.notice.service.NoticeService;
import com.eatmate.notice.vo.NoticePageForm;
import com.eatmate.notice.vo.NoticeSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/noticecompare")
public class RestNoticeController {

    private final NoticeService noticeService;


    // 모든 데이터를 가져오는 메서드 호출
    @GetMapping("/inefficient1")
    public ResponseEntity<Page<Notice>> getInefficientNoticesV1(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Notice> result = noticeService.findAllWithPageV1(pageable);
        return ResponseEntity.ok(result);
    }

    // 모든 데이터를 가져오는 메서드 호출
    @GetMapping("/inefficient2")
    public ResponseEntity<Page<Notice>> getInefficientNotices2(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        title = "10";

        Pageable pageable = PageRequest.of(page, size);
        Page<Notice> result = noticeService.findAllWithPageV2(title,pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/inefficient3")
    public List<Notice> getInefficientNoticesV3(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        title = "10";
        return noticeService.findAllWithPageV3(title, page, size);
    }


    @GetMapping("/inefficient4")
    public List<Notice> getInefficientNoticesV4(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size){

        title = "10";
        return noticeService.findAllWithPageV4(title, page, size);
    }

    @GetMapping("/inefficient5")
    public Page<NoticePageForm> getInefficientNoticesV5(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        title = (title == null || title.isEmpty()) ? "10" : title;

        return noticeService.findNoticeByTitleInDtoV5(title, page, size);
    }

    // 최적화된 검색 및 페이징 메서드 호출
    @GetMapping("/optimized")
    public ResponseEntity<Page<NoticePageForm>> getOptimizedNotices(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        NoticeSearchCondition condition = new NoticeSearchCondition();
        condition.setTitle(title);
        Pageable pageable = PageRequest.of(page, size);
        Page<NoticePageForm> result = noticeService.searchWithPage(condition, pageable);
        return ResponseEntity.ok(result);
    }

}
