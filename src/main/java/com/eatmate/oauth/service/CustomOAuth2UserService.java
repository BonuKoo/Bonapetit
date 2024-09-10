package com.eatmate.oauth.service;

import com.eatmate.account.service.AccountService;
import com.eatmate.domain.dto.AccountDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountService accountService;

    public CustomOAuth2UserService(AccountService accountService) {
        this.accountService = accountService;
    }

    //닉네임 불러오는거 추가 된 코드
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 기본적인 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 로그인 시 사용되는 provider 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String accessToken = userRequest.getAccessToken().getTokenValue(); // Access Token 가져오기

        // 세션에 Access Token 저장
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getSession();
        session.setAttribute("provider", registrationId);

        // 네이버 로그인 시 naverAccessToken 저장
        if ("naver".equals(registrationId)) {
            session.setAttribute("naverAccessToken", accessToken);  // 네이버 액세스 토큰을 세션에 저장
        } else {
            session.setAttribute("accessToken", accessToken);  // 일반적으로는 accessToken으로 저장
        }

        // OAuth2에서 제공하는 attributes 가져오기 (수정 가능하도록 복사)
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());

        String userNameAttributeKey;
        String oauth2Id;
        String nickname;

        // provider에 따라 사용자 ID와 닉네임 가져오기
        if ("naver".equals(registrationId)) {
            // 네이버 사용자 정보 처리
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            userNameAttributeKey = "id";
            oauth2Id = (String) response.get("id");
            nickname = (String) response.get("name");

            // 네이버는 'response' 내에 사용자 정보가 있으므로 attributes를 response로 교체
            attributes = new HashMap<>(response);
            attributes.put("id", oauth2Id);  // 'id' 필드를 attributes에 추가
            attributes.put("name", nickname); // 'name' 필드도 추가

        } else if ("kakao".equals(registrationId)) {
            // 카카오 사용자 정보 처리
            userNameAttributeKey = "id";
            oauth2Id = String.valueOf(attributes.get("id"));
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            nickname = (String) properties.get("nickname");

        } else {
            // 구글 등 다른 제공자 처리
            userNameAttributeKey = "sub";
            oauth2Id = (String) attributes.get("sub");
            nickname = (String) attributes.get("name");
        }

        // 계정 서비스에서 사용자 정보 확인 또는 새로 생성
        AccountDto accountDto = accountService.findByOauth2Id(oauth2Id);
        if (accountDto != null) {
            // 기존 사용자가 있으면 DB에서 닉네임 사용
            nickname = accountDto.getNick_name();
            accountDto.setAccess_token(accessToken); // Access Token 업데이트
            accountService.updateAccount(accountDto);
        } else {
            // 새로운 사용자의 경우 소셜에서 받은 닉네임으로 계정 생성
            accountDto = new AccountDto();
            accountDto.setEmail(oauth2Id);
            accountDto.setNick_name(nickname); // 최초 로그인 시 소셜 닉네임 저장
            accountDto.setPassword(oauth2Id);
            accountDto.setOauth2_id(oauth2Id);
            accountDto.setProvider(registrationId);
            accountDto.setAccess_token(accessToken);
            accountDto.setRoles("ROLE_USER");
            accountService.createAccount(accountDto);
        }

        // 최종적으로 attributes에 DB에서 조회한 닉네임을 넣음
        attributes.put("nickname", accountDto.getNick_name());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeKey  // 사용자 ID 필드로 'id'를 사용
        );
    }

    // 기존 로그인 코드
//    @Override
//    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
//        // 기본적인 사용자 정보 가져오기
//        OAuth2User oAuth2User = super.loadUser(userRequest);
//        // 로그인시 사용되는 provider 확인
//        String registrationId = userRequest.getClientRegistration().getRegistrationId();
//        String accessToken = userRequest.getAccessToken().getTokenValue(); // Access Token 가져오기
//        // 세션에 Access Token 저장
//        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getSession();
//        // 세션에 provider와 AccessToken 저장
//        session.setAttribute("provider", registrationId);
//        session.setAttribute("naverAccessToken", accessToken);
//
//        // 가져온 attributes 에서 필요한 데이터 추출을 위한 Map
////        Map<String, Object> attributes = oAuth2User.getAttributes();
//        // 가져온 attributes에서 필요한 데이터 추출
//        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes()); // 불변 맵을 수정 가능한 맵으로 복사
//        String userNameAttributeKey;
//        String oauth2Id = null;  // OAuth2 ID
//        String nickname = null;  // 닉네임
//
//        if ("naver".equals(registrationId)) {
//            // 네이버 사용자의 경우 'response' 안에 정보가 있음
//            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
//            userNameAttributeKey = "id";        // 네이버에서는 'id'를 사용자 식별자로 사용
//            oauth2Id = (String) response.get("id");
//            nickname = (String) response.get("name");
////            attributes = response;          // attributes를 'response'로 교체
//            attributes = new HashMap<>(response);  // 이 경우에도 수정 가능한 상태로 만들기
//
//            /*
//            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
//            attributes.put("id", response.get("id"));
//            attributes.put("name", response.get("name"));
//            userNameAttributeKey = (String) response.get("id");
//            oauth2Id = userNameAttributeKey;
//            response.get("name");
//            */
//
//        } else if ("kakao".equals(registrationId)) {
//            // 카카오는 사용자 ID와 닉네임을 가져오는 로직
//            userNameAttributeKey = "id"; // 카카오에서는 'id'를 사용자 식별자로 사용
//            oauth2Id = String.valueOf(attributes.get("id")); // 카카오는 Long 형태로 ID를 제공
//            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
//            nickname = (String) properties.get("nickname"); // 카카오에서는 'nickname'을 닉네임으로 사용
//        } else {
//            // 구글과 같은 다른 OAuth2 제공자는 'sub'를 식별자로 사용
//            userNameAttributeKey = "sub";
//            oauth2Id = (String) attributes.get("sub"); // 구글의 경우 'sub'가 사용자 식별자
//            nickname = (String) attributes.get("name");
//        }
//
//        // 닉네임을 attributes에 추가
//        attributes.put("nickname", nickname);
//
//        // 계정 서비스에서 사용자 정보 확인 또는 새로 생성
//        AccountDto accountDto = accountService.findByOauth2Id(oauth2Id);
//        if (accountDto != null) {
//            // 기존 사용자가 있으면 access_tokens 을 업데이트
//            accountDto.setAccess_token(accessToken);
//            accountService.updateAccount(accountDto);
//        } else {
//            // 새로운 사용자인 경우 계정 생성
//            accountDto = new AccountDto();
//            accountDto.setEmail(oauth2Id);
//            accountDto.setNick_name(nickname);
//            accountDto.setPassword(oauth2Id);
//            accountDto.setOauth2_id(oauth2Id);
//            accountDto.setProvider(registrationId);
//            accountDto.setAccess_token(accessToken);
//            accountDto.setRoles("ROLE_USER");
//            accountService.createAccount(accountDto);
//        }
//
//        return new DefaultOAuth2User(
//                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
//                attributes,
//                userNameAttributeKey
//        );
//    }

}
