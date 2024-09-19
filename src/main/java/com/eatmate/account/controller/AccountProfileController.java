package com.eatmate.account.controller;

import com.eatmate.account.service.AccountMyBatisService;
import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.dto.AccountTeamDto;
import com.eatmate.domain.dto.TeamDto;
import com.eatmate.post.service.PostTeamService;
import com.eatmate.weblogout.service.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profile")
@Slf4j
public class AccountProfileController {

    @Autowired
    private AccountMyBatisService accountMyBatisService;

    @Autowired
    private LogoutService logoutService;

    @Autowired
    private PostTeamService postTeamService;

    // 로그인한 유저 프로필 불러오기
    @GetMapping("/list")
    public String getProfilePage(Model model){
        // 인증 객체에서 현재 로그인한 사용자의 OAuth2 ID 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();

        // 로그인한 사용자 정보를 가져옴
        AccountDto dto = accountMyBatisService.findByOauth2Id(currentEmail);
        if (dto != null) {
            model.addAttribute("dto", dto);

            // 사용자가 속한 모든 팀을 가져옴
            List<TeamDto> teams = accountMyBatisService.getTeamsForUser(currentEmail);
            model.addAttribute("teams", teams); // 팀 리스트 추가

            // 사용자가 리더인 팀만 필터링하여 가져옴
            List<AccountTeamDto> leaderTeams = accountMyBatisService.getTeamsWhereIsLeader(currentEmail);
            model.addAttribute("leaderTeams", leaderTeams);

            // 리더가 아닌 팀 리스트 필터링
            List<TeamDto> nonLeaderTeams = teams.stream()
                    .filter(team -> leaderTeams.stream()
                            .noneMatch(leaderTeam -> leaderTeam.getTeam_id().equals(team.getTeam_id())))
                    .collect(Collectors.toList());
            model.addAttribute("nonLeaderTeams", nonLeaderTeams);

        } else {
            return "error"; // 사용자가 없는 경우 에러 페이지로 리다이렉트
        }
        return "account/profile/profileListForm";
    }

    // 회원 정보 수정
    @PostMapping("/detail")
    public String updateEmployee(@ModelAttribute AccountDto dto, HttpServletRequest request) {
        accountMyBatisService.updateDetailAccount(dto);

        // 현재 사용자 정보 갱신
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // attributes에 새로운 닉네임을 반영
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("nickname", dto.getNick_name());

        // 사용자 식별자 키를 결정하는 메서드 추가
        String userNameAttributeKey = getUserNameAttributeKey(authentication, request);

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

        // 세션에 provider 정보 저장
        HttpSession session = request.getSession();
        if (authentication instanceof OAuth2AuthenticationToken) {
            String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            session.setAttribute("provider", provider); // provider 정보를 세션에 저장
        }

        return "redirect:/";
    }

    // OAuth2 제공자에 따른 식별자 키를 가져오는 메서드 (회원 탈퇴시 필요)
    private String getUserNameAttributeKey(Authentication authentication, HttpServletRequest request) {
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
        // 만약 Authentication이 OAuth2가 아니라면 세션에서 provider를 가져옴
        HttpSession session = request.getSession(false);
        if (session != null) {
            String provider = (String) session.getAttribute("provider");
            if ("naver".equals(provider)) {
                return "id";
            } else if ("kakao".equals(provider)) {
                return "id";
            } else if ("google".equals(provider)) {
                return "sub";
            }
        }
        throw new IllegalArgumentException("Unknown provider");
    }

    // 회원 탈퇴
    @PostMapping("/delete")
    public String deleteAccount(@RequestParam("oauth2_id") String oauth2Id,
                                @RequestParam("account_id") String accountId,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 계정 삭제
        accountMyBatisService.deleteUserByOauth2Id(oauth2Id,accountId);
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

    // 팀 탈퇴 처리
    @PostMapping("/leaveTeam")
    public String leaveTeam(@RequestParam("account_id") String account_id,
                            @RequestParam("team_id") String team_id,
                            RedirectAttributes redirectAttributes) {
        // 팀에서 탈퇴 처리 (AccountTeam 관계 삭제)
        postTeamService.kickMember(account_id, team_id);

        // 탈퇴 성공 메시지 추가
        redirectAttributes.addFlashAttribute("message", "팀에서 성공적으로 탈퇴하였습니다.");

        // 홈으로 리다이렉트
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

