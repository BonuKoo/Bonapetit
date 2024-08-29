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

            // 로그인된 사용자의 정보를 이메일로 조회
            AccountDto accountDto = accountService.findByEmail(loggedInUserEmail);

            if (accountDto != null) {
                // 기존 회원이 존재하는 경우 OAuth2 ID와 Access Token 업데이트
                accountDto.setOauth2_id(attributes.get("id").toString());
                accountDto.setAccess_token(userRequest.getAccessToken().getTokenValue());
                accountService.updateAccount(accountDto);
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
