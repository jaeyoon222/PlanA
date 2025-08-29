package com.study.StudyCafe.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.text.Normalizer.normalize;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> normalized = normalize(provider, oAuth2User.getAttributes());
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                normalized,
                "id"
        );
    }
        private Map<String, Object> normalize (String provider, Map < String, Object > src){
            Map<String, Object> map = new HashMap<>();
            map.put("provider", provider);
            switch (provider) {
                case "google":
                    map.put("id", src.get("sub"));
                    map.put("email", src.get("email"));
                    map.put("name", src.get("name"));
                    map.put("profile_image", src.get("picture"));
                    break;
                case "kakao": {
                    map.put("id", src.get("id"));
                    Map<String, Object> kakaoAccount = (Map<String, Object>) src.getOrDefault("kakao_account", Map.of());
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());
                    map.put("email", kakaoAccount.get("email"));
                    map.put("name", profile.get("nickname"));
                    map.put("profile_image", profile.get("profile_image_url"));
                    break;
                }
                case "naver": {
                    Map<String, Object> resp = (Map<String, Object>) src.getOrDefault("response", Map.of());
                    map.put("id", resp.get("id"));
                    map.put("email", resp.get("email"));
                    map.put("name", resp.get("name"));
                    map.put("profile_image", resp.get("profile_image"));
                    break;
                }
                default:
                    map.putAll(src);
            }
            return map;
        }
    }