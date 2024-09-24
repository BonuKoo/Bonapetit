package com.eatmate.notice.controller;

import com.eatmate.notice.service.NoticeService;
import com.eatmate.notice.vo.NoticeForm;
import com.eatmate.notice.vo.NoticePageForm;
import com.eatmate.notice.vo.NoticeSearchCondition;
import com.eatmate.redis.config.dto.NoticePageFormCacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * Create
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/create")
    public String createPost(Model model){
        NoticeForm noticeForm = new NoticeForm();
        model.addAttribute("noticeForm",noticeForm);
        return "notice/createNoticeForm";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/create")
    public String createNotice(RedirectAttributes redirectAttributes,
            Principal principal,
            NoticeForm noticeForm){

            noticeForm.addAuthor(principal.getName());

        noticeService.createNotice(noticeForm);

        return "redirect:/notice";
    }

    /**
     * Read
     */
    //단 건
    @GetMapping("/detail/{noticeId}")
    public String details(@PathVariable Long noticeId, Model model){
        NoticeForm noticeForm = noticeService.findDetailById(noticeId);
        model.addAttribute("notice",noticeForm);
        return "notice/noticeView";
    }

    //페이지
    @GetMapping("")
    public String searchWithPage(@RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model){
        NoticeSearchCondition condition = new NoticeSearchCondition();
        condition.setTitle(keyword);

        Pageable pageable = PageRequest.of(page,size);

        Page<NoticePageForm> noticePage = noticeService.searchWithPage(condition, pageable);
        int startPage = ((page / 10) * 10) + 1;
        int endPage = Math.min(startPage + 9, noticePage.getTotalPages());

        model.addAttribute("notice", noticePage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("lastPage", noticePage.getTotalPages());
        return "notice/list";    }

    //수정 페이지
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/update/{id}")
    public String modifyNotice(@PathVariable Long id, Model model){
        NoticeForm noticeForm = noticeService.findDetailById(id);
        model.addAttribute("notice",noticeForm);
        return "notice/update";
    }
    //수정
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{id}")
    public String modifyNotice(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam String content){
        noticeService.updateNotice(id, title, content);
        return "redirect:/notice/detail/" + id;
    }
    //삭제
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/delete/{id}")
    public String deleteNotice(@PathVariable Long id){
        noticeService.removeNotice(id);
        return "redirect:/notice";
    }

}
