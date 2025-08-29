package com.study.StudyCafe.config;

import com.study.StudyCafe.constant.UserRole;
import com.study.StudyCafe.entity.User;
import com.study.StudyCafe.repository.UserRepository;
import com.study.StudyCafe.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attr = principal.getAttributes();

        String provider = (String) attr.getOrDefault("provider", "unknown");
        String providerId = String.valueOf(attr.get("id"));
        String email = (String) attr.getOrDefault("email", null);
        String name = (String) attr.getOrDefault("name", "사용자");
        String profileImage = (String) attr.getOrDefault("profile_image", null);

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(name)
                        .profileImage(profileImage)
                        .provider(provider)
                        .providerId(providerId)
                        .role(UserRole.USER)
                        .build()));

        Map<String, Object> claims = Map.of(
                "uid", user.getId(),
                "provider", provider,
                "profile_image", profileImage
        );

        String subject = (email != null && !email.isBlank()) ? email : provider + ":" + providerId;
        String accessToken = jwtUtil.generateAccessToken(subject, user.getRole().name(), claims);
        String refreshToken = jwtUtil.generateRefreshToken(subject);

        // 프론트 /login 으로 리다이렉트. 환경변수 FRONT_LOGIN_URL 우선
        String redirectUrl = System.getenv().getOrDefault("FRONT_LOGIN_URL", "http://14.37.8.141:3000/login");
        String url = redirectUrl
                + "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        response.sendRedirect(url);
    }
}
