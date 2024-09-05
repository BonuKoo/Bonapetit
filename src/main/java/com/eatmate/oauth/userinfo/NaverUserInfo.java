package com.eatmate.oauth.userinfo;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    // 생성자에서 네이버 response 필드를 처리
    @SuppressWarnings("unchecked")
    public NaverUserInfo(Map<String, Object> attributes) {
        // response 안에 있는 데이터를 attributes로 설정
        this.attributes = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
}