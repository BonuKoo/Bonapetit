package com.eatmate.kakao.controller;

import com.eatmate.kakao.service.KakaoService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class KakaoController {

    @Autowired
    private KakaoService kakaoService;

    @GetMapping("/oauth/kakao/callback")
    public String kakaoCallback(@RequestParam String code, Model model){
        // 인증 코드로 액세스 토근 요청
        String accessToken = kakaoService.getAccessToken(code);
        System.out.println(accessToken);
        // 액세스 토큰으로 사용자 정보 요청
        Map<String, Object> userProfile = kakaoService.getUserProfile(accessToken);
        System.out.println("lgoin Controller : "+userProfile);
        // 사용자 정보 모델에 추가
        model.addAttribute("userProfile",userProfile);
        // 뷰 이동
        return "account/userProfile";
    }
}