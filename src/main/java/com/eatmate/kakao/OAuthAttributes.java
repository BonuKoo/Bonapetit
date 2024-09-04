package com.eatmate.kakao;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private final String name;
    private final String email;
    private final Map<String, Object> attributes;
    private final String provider;
    private final String providerId;
    private final String nameAttributeKey;

    @Builder
    public OAuthAttributes(String name, String email, Map<String, Object> attributes, String provider, String providerId, String nameAttributeKey) {
        this.name = name;
        this.email = email;
        this.attributes = attributes;
        this.provider = provider;
        this.providerId = providerId;
        this.nameAttributeKey = nameAttributeKey;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName,
                                     Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            return ofNaver(registrationId, userNameAttributeName, attributes);
//            return ofNaver(registrationId, attributes);
        } else if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }

        // Default to Google or others
        return ofGoogle(registrationId, userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) ((Map<String, Object>) attributes.get("properties")).get("nickname"))
                .email((String) ((Map<String, Object>) attributes.get("kakao_account")).get("email"))
                .attributes(attributes)
                .provider("kakao")
                .providerId(String.valueOf(attributes.get("id")))
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        // attributes 안에 'response'가 있는지 확인
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        if (response == null) {
            throw new OAuth2AuthenticationException("Missing 'response' in Naver attributes");
        }

        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
//                .attributes(attributes)
                .attributes(response)
                .provider("naver")
//                .provider(registrationId)
                .providerId((String) response.get("id"))
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofGoogle(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .attributes(attributes)
                .provider(registrationId)
                .providerId((String) attributes.get("sub"))
                .nameAttributeKey(userNameAttributeName)
                .build();
    }
}
