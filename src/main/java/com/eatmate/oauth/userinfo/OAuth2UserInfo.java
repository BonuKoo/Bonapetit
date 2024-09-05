package com.eatmate.oauth.userinfo;

public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getName();
}

