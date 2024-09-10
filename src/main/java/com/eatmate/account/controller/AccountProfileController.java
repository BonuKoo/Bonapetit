package com.eatmate.account.controller;

import com.eatmate.account.service.AccountService;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.dto.AccountTeamDto;
import com.eatmate.domain.dto.TeamDto;
import com.eatmate.weblogout.service.LogoutService;
import feign.Param;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/profile")
@Slf4j
public class AccountProfileController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private LogoutService logoutService;

    // 닉네임 변경 및 팀 리스트
    @GetMapping("/list")
    public String getProfilePage(Model model){
        // 인증 객체에서 현재 로그인한 사용자의 OAuth2 ID 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();

        AccountDto dto = accountService.findByOauth2Id(currentEmail);
        if (dto != null) {
            model.addAttribute("dto", dto);

            // 본인이 속한 팀 리스트 가져오기
            List<TeamDto> teams = accountService.getTeamsForUser(currentEmail);
            model.addAttribute("teams", teams); // 팀 리스트 추가

            // 본인이 개설한 팀(리더인 팀) 리스트 가져오기
            List<AccountTeamDto> leaderTeams = accountService.getTeamsWhereIsLeader(currentEmail);
            model.addAttribute("leaderTeams", leaderTeams);
        } else {
            return "error"; // 사용자가 없는 경우 에러 페이지로 리다이렉트 (원하는 대로 변경 가능)
        }
        return "account/profile/profileListForm";
    }

    // 회원 정보 수정
    @PostMapping("/detail")
    public String updateEmployee(@ModelAttribute AccountDto dto) {
        accountService.updateDetailAccount(dto);

        // 현재 사용자 정보 갱신
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // attributes에 새로운 닉네임을 반영
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("nickname", dto.getNick_name());

        // 사용자 식별자 키를 결정하는 메서드 추가
        String userNameAttributeKey = getUserNameAttributeKey(authentication); // 여기서 수정

        // 새로운 DefaultOAuth2User 객체 생성
        OAuth2User updatedUser = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeKey // 사용자의 식별자 key
        );

        // 새로운 Authentication 객체로 교체
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                updatedUser,
                authentication.getCredentials(),
                authentication.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        return "redirect:/";
    }

    // OAuth2 제공자에 따른 식별자 키를 가져오는 메서드
    private String getUserNameAttributeKey(Authentication authentication) {
        // OAuth2AuthenticationToken에서 제공자의 registrationId를 가져옴
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
            String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();

            // 각 제공자에 따른 식별자 key를 반환
            if ("naver".equals(registrationId)) {
                return "id"; // 네이버는 'id' 사용
            } else if ("kakao".equals(registrationId)) {
                return "id"; // 카카오는 'id' 사용
            } else if ("google".equals(registrationId)) {
                return "sub"; // 구글은 'sub' 사용
            }
        }
        throw new IllegalArgumentException("Unknown provider");
    }

    // 회원 탈퇴
    @PostMapping("/delete")
    public String deleteAccount(@RequestParam("oauth2_id") String oauth2Id,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 계정 삭제
        accountService.deleteUserByOauth2Id(oauth2Id);
        redirectAttributes.addFlashAttribute("message", "회원 탈퇴가 완료되었습니다.");

        // 세션에서 provider 정보 가져오기
        HttpSession session = request.getSession(false);
        String provider = (session != null) ? (String) session.getAttribute("provider") : null;

        // 인증 객체 가져오기
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            // 각 provider별로 로그아웃 처리
            if (provider != null) {
                switch (provider) {
                    case "kakao":
                        // 카카오 로그아웃 처리
                        String kakaoAccessToken = (String) session.getAttribute("kakaoAccessToken");
                        if (kakaoAccessToken != null) {
                            logoutService.kakaoLogout(kakaoAccessToken);  // 카카오 로그아웃
                        }
                        break;

                    case "naver":
                        // 네이버 로그아웃 처리
                        String naverAccessToken = (String) session.getAttribute("naverAccessToken");
                        if (naverAccessToken != null) {
                            logoutService.naverLogout(naverAccessToken);  // 네이버 로그아웃
                        }
                        break;

                    case "google":
                        // 구글 로그아웃 처리
                        session.invalidate();  // 세션 무효화
                        return "redirect:https://accounts.google.com/logout";  // 구글 로그아웃 URL로 리다이렉트
                }
            }

            // 일반 로그인(로컬) 로그아웃 처리 또는 세션 무효화
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        // 로그아웃 후 홈 페이지로 리다이렉트
        return "redirect:/";
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
