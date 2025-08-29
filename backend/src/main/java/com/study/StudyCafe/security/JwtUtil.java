package com.study.StudyCafe.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms:1209600000}")
    private long refreshExpirationMs;

    @Value("${jwt.clock-skew-seconds:60}")
    private long clockSkewSeconds;

    @Value("${jwt.issuer:studycafe}")
    private String issuer;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // ===== 발급 =====
    public String generateAccessToken(String subject, String role, Map<String, Object> extraClaims) {
        long now = System.currentTimeMillis();
        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)                         // ★ 중요: issuer 세팅
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(getSigningKey());

        if (extraClaims != null && !extraClaims.isEmpty()) {
            builder.addClaims(extraClaims);
        }
        return builder.compact();
    }

    public String generateRefreshToken(String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)                         // ★ 중요
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + refreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ===== 파싱/검증 =====
    private Jws<Claims> parseJws(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setAllowedClockSkewSeconds(clockSkewSeconds)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token);
    }

    public Claims getAllClaims(String token) {
        return parseJws(token).getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseJws(token);                 // ★ parseClaims → parseJws로 교체하면 issuer/clock skew 반영
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return getAllClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        Object role = getAllClaims(token).get("role");
        return role == null ? null : role.toString();
    }

    // ★ 추가: 필터에서 쓰기 좋은 헬퍼
    public Claims validateAndGetClaims(String token) {
        try {
            return parseJws(token).getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
