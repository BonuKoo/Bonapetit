package com.eatmate.account.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class AccountProfileController {


    //회원 정보 페이지
    @GetMapping("/detail")
    public String getDetailPage() {
        return "account/profile/detailAccountForm";
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
