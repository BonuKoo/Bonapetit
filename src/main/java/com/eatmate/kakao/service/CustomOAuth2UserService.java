package com.eatmate.kakao.service;

import com.eatmate.account.service.AccountService;
import com.eatmate.domain.dto.AccountDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;

public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountService accountService;

    public CustomOAuth2UserService(AccountService accountService){
        this.accountService = accountService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 현재 로그인된 사용자의 이메일을 가져옵니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String loggedInUserEmail = authentication.getName(); // 현재 로그인된 사용자의 이메일

            // 인증된 사용자 로그 출력
            System.out.println("현재 인증된 사용자: " + loggedInUserEmail);

            // 로그인된 사용자의 정보를 이메일로 조회
            AccountDto accountDto = accountService.findByEmail(loggedInUserEmail);

            if (accountDto != null) {
                // 기존 회원이 존재하는 경우 OAuth2 ID와 Access Token 업데이트
                accountDto.setOauth2_id(attributes.get("id").toString());
                accountDto.setAccess_token(userRequest.getAccessToken().getTokenValue());

                System.out.println("업데이트 전 AccountDto: " + accountDto);

                boolean updated = accountService.updateAccount(accountDto);

                if (updated) {
                    System.out.println("카카오 정보 업데이트 성공");
                } else {
                    System.out.println("카카오 정보 업데이트 실패");
                }
            } else {
                System.out.println("계정 정보를 찾을 수 없습니다.");
            }
        } else {
            // 인증되지 않은 상태에서의 처리
            throw new OAuth2AuthenticationException("User is not authenticated");
        }

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id");
    }

}
