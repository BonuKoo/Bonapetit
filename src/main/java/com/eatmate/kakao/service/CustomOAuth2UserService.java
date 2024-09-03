package com.eatmate.kakao.service;

import com.eatmate.account.service.AccountService;
import com.eatmate.domain.dto.AccountDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountService accountService;

    public CustomOAuth2UserService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        System.out.println("Attributes: " + attributes);  // 전체 attributes 로그 출력

        String oauth2Id = null;
        String email = null;
        String nickname = null;

        if ("kakao".equals(registrationId)) {
            oauth2Id = String.valueOf(attributes.get("id"));
            email = (String) attributes.get("email");
            nickname = (String) attributes.get("nickname");
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            if (response != null) {
                System.out.println("Naver Response: " + response);  // 네이버 response 로그 출력
                // JSON 구조 확인 후 이 부분을 조정하세요.
                oauth2Id = (String) response.get("id");
                email = (String) response.get("email");
                nickname = (String) response.get("nickname");
            }
        }

        if (oauth2Id == null) {
            throw new OAuth2AuthenticationException("OAuth2 ID not found in attributes.");
        }

        // 사용자 정보 저장 또는 업데이트
        AccountDto accountDto = accountService.findByOauth2Id(oauth2Id);
        if (accountDto != null) {
            accountDto.setOauth2_id(oauth2Id);
            accountDto.setAccess_token(userRequest.getAccessToken().getTokenValue());
            accountService.updateAccount(accountDto);
        } else {
            accountDto = new AccountDto();
            accountDto.setEmail(email);
            accountDto.setNick_name(nickname);
            accountDto.setPassword(oauth2Id); // 비밀번호는 OAuth2 ID로 설정 (실제로는 별도 처리 필요)
            accountDto.setOauth2_id(oauth2Id);
            accountDto.setAccess_token(userRequest.getAccessToken().getTokenValue());
            accountDto.setRoles("ROLE_USER");
            accountService.createAccount(accountDto);
        }

        Collection<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new DefaultOAuth2User(authorities, attributes, userNameAttributeName);
    }



    // 카카오 구현 했던 기존 코드(9/3)
    /*@Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 카카오 사용자 ID 가져오기
        Long OAuth2Id = (Long) attributes.get("id");
        String kakaoId = String.valueOf(OAuth2Id);
        if (OAuth2Id == null) {
            throw new OAuth2AuthenticationException("Kakao ID not found in OAuth2 attributes.");
        }

        // 카카오 ID로 사용자 정보 조회
        AccountDto accountDto = accountService.findByOauth2Id(kakaoId);

        if (accountDto != null) {
            // 기존 계정이 존재하는 경우 업데이트
            accountDto.setOauth2_id(kakaoId);
            accountDto.setAccess_token(userRequest.getAccessToken().getTokenValue());

            boolean updated = accountService.updateAccount(accountDto);

            if (updated) {
                System.out.println("카카오 정보 업데이트 성공");
            } else {
                System.out.println("카카오 정보 업데이트 실패");
            }
        } else {
            // 계정이 존재하지 않는 경우
            System.out.println("계정 정보를 찾을 수 없습니다. 새로운 계정을 생성합니다.");

            // 새로운 계정 생성 로직
            String nickname = (String) attributes.get("nickname"); // 마찬가지로 닉네임도 가져올 수 있다면

            accountDto = new AccountDto();
            accountDto.setEmail(kakaoId);
            accountDto.setNick_name(nickname);
            accountDto.setPassword(kakaoId);
            accountDto.setOauth2_id(kakaoId);
            accountDto.setAccess_token(userRequest.getAccessToken().getTokenValue());
            accountDto.setRoles("ROLE_USER"); // 기본 권한 설정

            boolean created = accountService.createAccount(accountDto);

            if (created) {
                System.out.println("새로운 계정이 성공적으로 생성되었습니다.");
            } else {
                throw new OAuth2AuthenticationException("Failed to create new account with Kakao information.");
            }
        }

        // 사용자 권한 설정
        Collection<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new DefaultOAuth2User(
                authorities,
                attributes,
                "id"
        );
    }*/
}
