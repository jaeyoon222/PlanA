package com.study.StudyCafe.controller;

import com.study.StudyCafe.dto.user.UserRegisterDto;
import com.study.StudyCafe.dto.user.UserResponseDto;
import com.study.StudyCafe.entity.User;
import com.study.StudyCafe.repository.UserRepository;
import com.study.StudyCafe.security.JwtUtil;
import com.study.StudyCafe.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;
    private final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.get("email"),
                            request.get("password")
                    )
            );
            String role = auth.getAuthorities().stream()
                    .findFirst().map(a -> a.getAuthority()).orElse("ROLE_USER");
            String accessToken = jwtUtil.generateAccessToken(auth.getName(), role, null);
            String refreshToken = jwtUtil.generateRefreshToken(auth.getName());
            return ResponseEntity.ok(Map.of("accessToken", accessToken, "refreshToken", refreshToken));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "등록되지 않은 이메일입니다."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "비밀번호가 일치하지 않습니다."));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "인증 실패"));
        }
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "휴대폰 번호는 필수입니다."));
        }
        userService.sendVerificationCode(phone);
        return ResponseEntity.ok(Map.of("message", "인증번호가 전송되었습니다."));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody UserRegisterDto dto,
            @RequestParam("code") String code
    ) {
        userService.registerUserWithVerification(dto, code);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "회원가입 성공"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "유효하지 않은 리프레시 토큰"));
        }
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String role = jwtUtil.getRoleFromToken(refreshToken); // 없다면 'ROLE_USER' 등 기본값 사용
        String newAccessToken = jwtUtil.generateAccessToken(username, role, null);
        // (권장) 롤링
        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "인증 필요"));
        }

        String email = authentication.getName(); // == getUsername()
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 정보 없음"));

        return ResponseEntity.ok(new UserResponseDto(user));
    }
}
