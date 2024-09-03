package com.eatmate.account.controller;

import com.eatmate.account.service.AccountService;
import com.eatmate.domain.dto.AccountDto;
import feign.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/profile")
public class AccountProfileController {

    @Autowired
    private AccountService accountService;

    //회원 정보 페이지
    @GetMapping("/detail")
    public String getDetailPage(Model model) {
        // 인증 객체에서 현재 로그인한 사용자의 OAuth2 ID 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();

        AccountDto dto = accountService.findByOauth2Id(currentEmail);
        if (dto != null) {
            model.addAttribute("dto", dto);
        } else {
            return "error"; // 사용자가 없는 경우 에러 페이지로 리다이렉트 (원하는 대로 변경 가능)
        }

        return "account/profile/detailAccountForm"; // 리다이렉트가 아닌 뷰 반환
    }

    // 회원 정보 수정
    @PostMapping("/detail")
    public String updateEmployee(@ModelAttribute AccountDto dto) {
        accountService.updateDetailAccount(dto);
        return "redirect:/";
    }

    // 회원 탈퇴
    @PostMapping("/delete")
    public String deleteAccount(@RequestParam("oauth2_id") String oauth2Id, RedirectAttributes redirectAttributes) {
        accountService.deleteUserByOauth2Id(oauth2Id);
        redirectAttributes.addFlashAttribute("message", "회원 탈퇴가 완료되었습니다.");
        return "redirect:/";
    }

    //등록된 모임 리스트 페이지
    @GetMapping("/joinedTeam")
    public String getJoinedListPage() {
        return "account/profile/listJoinedTeamForm";
    }

    //신청한 모임 리스트 페이지
    @GetMapping("/appliedTeam")
    public String getAppliedTeamListPage() {
        return "account/profile/listApplyTeamForm";
    }
    
    //활성된 채팅방 리스트 페이지
    @GetMapping("/chatRoom")
    public String getChatRoomListPage() {
        return "account/profile/listChatroomForm";
    }

}
