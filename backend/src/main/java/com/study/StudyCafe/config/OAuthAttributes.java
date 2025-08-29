package com.study.StudyCafe.config;

import java.util.HashMap;
import java.util.Map;

public class OAuthAttributes {

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        } else if ("naver".equals(registrationId)) {
            return ofNaver(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return new OAuthAttributes(attributes, userNameAttributeName);
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        Map<String, Object> map = new HashMap<>();
        map.put("id", attributes.get("id"));
        map.put("nickname", profile.get("nickname"));
        map.put("email", kakaoAccount.get("email"));
        map.put("profile_image", profile.get("profile_image_url"));

        return new OAuthAttributes(map, userNameAttributeName);
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        Map<String, Object> map = new HashMap<>();
        map.put("id", response.get("id"));
        map.put("nickname", response.get("name"));
        map.put("email", response.get("email"));
        map.put("profile_image", response.get("profile_image"));

        return new OAuthAttributes(map, userNameAttributeName);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public String getNameAttributeKey() {
        return nameAttributeKey;
    }

    public String getEmail() {
        return (String) attributes.get("email");
    }

    public String getId() {
        return String.valueOf(attributes.get("id"));
    }
}
