package com.eatmate.account.controller;

import com.eatmate.account.service.AccountJpaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AccountJpaController {

    private AccountJpaService accountService;


}
