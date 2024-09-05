//package com.eatmate.weblogout.controller;
//
//import com.eatmate.account.service.AccountService;
//import com.eatmate.domain.dto.AccountDto;
//import com.eatmate.weblogout.service.KakaoService;
//import jakarta.servlet.http.HttpServletResponse;
//import jakarta.servlet.http.HttpSession;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.io.IOException;
//import java.util.Map;
//
//@Controller
//public class KakaoController {
//
//    @Autowired
//    private KakaoService kakaoService;
//
//    @Autowired
//    private AccountService accountService;  // AccountService 추가
//
//    @GetMapping("/oauth/kakao/callback")
//    public String kakaoCallback(@RequestParam String code, Model model, HttpSession session) {
//        // 인증 코드로 액세스 토큰 요청
//        String accessToken = kakaoService.getAccessToken(code);
//        session.setAttribute("kakaoAccessToken", accessToken);
//
//        // 세션에서 이메일 가져오기
//        String userEmail = (String) session.getAttribute("userEmail");
//        if (userEmail == null) {
//            System.out.println("No email found in session. User must be logged in first.");
//            return "redirect:/login";  // 로그인 페이지로 리다이렉트
//        }
//
//        // 사용자 프로필 정보에서 카카오 ID 및 토큰 추출
//        Map<String, Object> userProfile = kakaoService.getUserProfile(accessToken);
//        String kakaoId = userProfile.get("id").toString();
//
//        // DB에서 사용자 조회 및 업데이트
//        AccountDto accountDto = accountService.findByEmail(userEmail);
//        if (accountDto != null) {
//            accountDto.setOauth2_id(kakaoId);
//            accountDto.setAccess_token(accessToken);
//            boolean updated = accountService.updateAccount(accountDto);
//            if (updated) {
//                System.out.println("User information successfully updated with Kakao ID and Access Token.");
//            } else {
//                System.out.println("Failed to update user information.");
//            }
//        } else {
//            System.out.println("User not found with email: " + userEmail);
//        }
//
//        model.addAttribute("userProfile", userProfile);
//        return "redirect:/";
//    }
//
//
//    // 카카오 로그아웃
//    @GetMapping("/kakao/logout")
//    public void logout(HttpServletResponse response, HttpSession session) throws IOException {
//        String accessToken = (String) session.getAttribute("kakaoAccessToken");
//
//        // 액세스 토큰 확인을 위해 로그 출력
//        System.out.println("액세스 토큰: " + accessToken);
//
//        if (accessToken != null){
//            // 액세스 토큰 무효화
//            kakaoService.logout(accessToken);
//            session.removeAttribute("kakaoAccessToken");
//            session.invalidate();   // 세션 무효화
//        }
//        // 카카오 로그아웃 URL로 리다이렉트
//        String requestUrl = "https://kauth.kakao.com/oauth/logout"
//                + "?client_id=e14cb05b33510d6d6fb59bc77f202156"
//                + "&logout_redirect_uri=http://localhost:8080";     // 카카오 로그아웃에 저장한 url
//
//        response.sendRedirect(requestUrl);
//    }
//
//}