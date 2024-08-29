package com.eatmate.kakao.service;

import com.eatmate.domain.dto.AccountDto;
import com.eatmate.domain.entity.user.Account;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;

public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    /*
    private final AccountRepository accountRepository;

    public CustomOAuth2UserService(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }
    */
    /*
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String oauth2Id = attributes.get("id").toString();
        String email = ((Map<String, Object>) attributes.get("kakao_account")).get("email").toString();
        String nickname = ((Map<String, Object>) attributes.get("properties")).get("nickname").toString();

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName);
    }
    private Account registerNewAccount(AccountDto accountDto){
        Account account = Account.fromDto(accountDto);
        return accountRepository.save(account);
    }
    */
}





