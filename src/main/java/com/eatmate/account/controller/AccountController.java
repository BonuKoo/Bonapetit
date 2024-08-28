package com.eatmate.account.controller;

import com.eatmate.dao.mybatis.AccountDao;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.entity.user.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
public class AccountController {

    @Autowired
    private AccountDao dao;

    // 로그인 페이지
    @GetMapping("/login")
    public String login() {
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
        //log.info("dto email parameter : {}", dto.);
        dao.insertjoin(dto);
        return "redirect:/login";
    }
}
