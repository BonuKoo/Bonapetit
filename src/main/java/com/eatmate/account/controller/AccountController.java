package com.eatmate.account.controller;

import com.eatmate.account.service.AccountMyBatisService;
import com.eatmate.account.service.AccountService;
import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.domain.dto.AccountDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AccountController {

    private final AccountMyBatisService myBatisService;
    private final AccountService accountService;

    @Autowired
    private AccountDao dao;

    // 로그인 페이지
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "exception", required = false) String exception, Model model) {
        model.addAttribute("error",error);
        model.addAttribute("exception",exception);
        return "account/login";
    }

    // 회원가입 페이지
    @GetMapping("/join")
    public String JoinPage(){
        return "account/join";
    }

    // 회원가입 정보 DB로 넘기기
    @PostMapping("/join") //DB에 저장
    public String joinAply(AccountDto dto) {
        myBatisService.join(dto);

        log.info("dto:: {}", dto.getEmail());

        return "redirect:/login";
    }

//    @GetMapping("/logout")
//    private String logout(HttpServletRequest request, HttpServletResponse response){
//        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
//        if (authentication != null) {
//            new SecurityContextLogoutHandler().logout(request, response, authentication);
//        }
//        return "redirect:/login";
//    }

    @GetMapping("/logout")
    private String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            // Authentication에서 이메일을 가져오기
            String email = authentication.getName();

            // 이메일로 계정 조회 후 로그아웃 처리
            AccountDto accountDto = accountService.findByEmail(email);
            if (accountDto != null) {
                // 로그아웃 처리
                new SecurityContextLogoutHandler().logout(request, response, authentication);
            }
        }
        return "redirect:/login";
    }

}
