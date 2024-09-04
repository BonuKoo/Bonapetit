package com.eatmate.kakao;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        // 카카오에서는 id가 최상위에 위치
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getEmail() {
        // 이메일은 kakao_account 내에 존재
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    public String getName() {
        // 닉네임은 properties나 kakao_account 내 profile에 있을 수 있음
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (properties != null && properties.get("nickname") != null) {
            return (String) properties.get("nickname");
        } else if (kakaoAccount != null && kakaoAccount.get("profile") != null) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            return (String) profile.get("nickname");
        }
        return null;
    }
}
