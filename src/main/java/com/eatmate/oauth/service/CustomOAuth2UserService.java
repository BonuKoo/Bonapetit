package com.eatmate.oauth.service;

import com.eatmate.account.service.AccountService;
import com.eatmate.domain.dto.AccountDto;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

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
        // 기본적인 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);
        // 로그인시 사용되는 provider 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        // 가져온 attributes 에서 필요한 데이터 추출을 위한 Map
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String userNameAttributeKey;
        String oauth2Id = null;  // OAuth2 ID
        String nickname = null;  // 닉네임

        if ("naver".equals(registrationId)) {
            // 네이버 사용자의 경우 'response' 안에 정보가 있음

            Map<String, Object> reaponse = (Map<String, Object>) attributes.get("response");

            /*  TODO :: 소중한 굴러가는 코드임 나중에 주석 풀어야함  */
            userNameAttributeKey = "id";        // 네이버에서는 'id'를 사용자 식별자로 사용
            oauth2Id = (String) reaponse.get("id");
            nickname = (String) reaponse.get("name");
            attributes = reaponse;          // attributes를 'response'로 교체

            /*
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            attributes.put("id", response.get("id"));
            attributes.put("name", response.get("name"));
            userNameAttributeKey = (String) response.get("id");
            oauth2Id = userNameAttributeKey;
            response.get("name");
            */

        } else if ("kakao".equals(registrationId)) {
            // 카카오는 사용자 ID와 닉네임을 가져오는 로직
            userNameAttributeKey = "id"; // 카카오에서는 'id'를 사용자 식별자로 사용
            oauth2Id = String.valueOf(attributes.get("id")); // 카카오는 Long 형태로 ID를 제공
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            nickname = (String) properties.get("nickname"); // 카카오에서는 'nickname'을 닉네임으로 사용
        } else {
            // 구글과 같은 다른 OAuth2 제공자는 'sub'를 식별자로 사용
            userNameAttributeKey = "sub";
            oauth2Id = (String) attributes.get("sub"); // 구글의 경우 'sub'가 사용자 식별자
            nickname = (String) attributes.get("name");
        }

        // 계정 서비스에서 사용자 정보 확인 또는 새로 생성
        AccountDto accountDto = accountService.findByOauth2Id(oauth2Id);
        if (accountDto != null) {
            // 기존 사용자가 있으면 access_tokens 을 업데이트
            accountDto.setAccess_token(userRequest.getAccessToken().getTokenValue());
            accountService.updateAccount(accountDto);
        } else {
            // 새로운 사용자인 경우 계정 생성
            accountDto = new AccountDto();
            accountDto.setEmail(oauth2Id);      //email 제거 예정
            accountDto.setNick_name(nickname);
            accountDto.setPassword(oauth2Id);
            accountDto.setOauth2_id(oauth2Id);
            accountDto.setProvider(registrationId);
            accountDto.setAccess_token(userRequest.getAccessToken().getTokenValue());
            accountDto.setRoles("ROLE_USER");
            accountService.createAccount(accountDto);
        }

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeKey
        );
    }
}


