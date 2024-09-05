package com.eatmate.oauth.userinfo;

import java.util.Map;

public class GoogleUserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        // 구글에서는 'sub'가 사용자 식별자로 사용됩니다.
        return String.valueOf(attributes.get("sub"));
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getName() {
        // 구글에서는 'name' 속성이 사용자 이름입니다.
        return (String) attributes.get("name");
    }

}
