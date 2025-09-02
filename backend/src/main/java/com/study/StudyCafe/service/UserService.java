package com.study.StudyCafe.service;

import com.study.StudyCafe.constant.UserRole;
import com.study.StudyCafe.dto.user.UserRegisterDto;
import com.study.StudyCafe.dto.user.UserUpdateDto;
import com.study.StudyCafe.entity.User;
import com.study.StudyCafe.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;

    // ✅ 인증코드 객체
    private static class VerificationData {
        String code;
        LocalDateTime createdAt;

        VerificationData(String code) {
            this.code = code;
            this.createdAt = LocalDateTime.now();
        }

        boolean isExpired() {
            return Duration.between(createdAt, LocalDateTime.now()).toMinutes() > 5;
        }
    }

    private final Map<String, VerificationData> verificationCodes = new ConcurrentHashMap<>();

    // ✅ 인증번호 전송
    public void sendVerificationCode(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("휴대폰 번호가 비어 있습니다.");
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        verificationCodes.put(phoneNumber, new VerificationData(code));

        String message = "[Plan A] 본인 인증 요청\n인증번호: " + code + "\n(5분 내 입력)";
        log.info("📨 문자 내용: {}", message);
        smsService.sendSMS(phoneNumber, message);
    }

    // ✅ 인증번호 검증
    public boolean verifyCode(String phoneNumber, String inputCode) {
        VerificationData data = verificationCodes.get(phoneNumber);
        if (data == null || data.isExpired()) return false;
        return data.code.equals(inputCode);
    }

    // ✅ 회원가입 시 인증번호 확인
    @Transactional
    public User registerUserWithVerification(UserRegisterDto dto, String code) {
        log.info("🟡 registerUserWithVerification() 호출됨");
        log.info("📩 받은 코드: {}", code);
        log.info("📨 DTO 내용:");
        log.info(" - 이메일: {}", dto.getEmail());
        log.info(" - 닉네임: {}", dto.getNickname());
        log.info(" - 이름: {}", dto.getName());
        log.info(" - 생일: {}", dto.getBirth());
        log.info(" - 전화번호: {}", dto.getPhone());
        log.info(" - 주소: {}", dto.getAddress());
        log.info(" - 프로필 이미지: {}", dto.getProfileImage());
        log.info(" - 비밀번호: {}", dto.getPassword());

        if (!verifyCode(dto.getPhone(), code)) {
            log.warn("❌ 인증번호 검증 실패");
            throw new IllegalArgumentException("인증번호가 올바르지 않습니다.");
        }

        log.info("✅ 인증번호 검증 성공");

        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .nickname(dto.getNickname())
                .birth(dto.getBirth())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .profileImage(dto.getProfileImage())
                .provider("local")
                .role(UserRole.USER)
                .build();

        log.info("💾 저장할 User 객체: {}", user);

        User saved = saveUser(user);

        log.info("🎉 저장된 User ID: {}", saved.getId());
        return saved;
    }

    // ✅ 이메일 중복 체크
    private void validateDuplicateUser(String email, String phone) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일이 비어 있습니다.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        if (phone != null && !phone.isBlank() && userRepository.existsByPhone(phone)) {
            throw new IllegalStateException("이미 등록된 전화번호입니다.");
        }
    }


    // ✅ 일반 회원가입
    public User registerUser(UserRegisterDto dto) {
        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .nickname(dto.getNickname())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .profileImage(dto.getProfileImage())
                .birth(dto.getBirth())
                .provider("local")
                .role(UserRole.USER)
                .build();
        return saveUser(user);
    }

    private User saveUser(User user) {
        validateDuplicateUser(user.getEmail(),user.getPhone());
        User saved = userRepository.save(user);
        userRepository.flush();  // 즉시 DB에 반영
        log.info("✅ saveUser(): 사용자 저장 완료 -> {}", saved.getEmail());
        return saved;
    }

    // ✅ 유저 정보 수정
    public void updateUserByEmail(String email, UserUpdateDto dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isSocialUser()) {
            String phoneToVerify = dto.getPhone() != null ? dto.getPhone() : user.getPhone();
            if (!verifyCode(phoneToVerify, dto.getVerificationCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다.");
            }
        } else {
            if (dto.getCurrentPassword() == null || !passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");
            }

            if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            }
        }

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getAddress() != null) user.setAddress(dto.getAddress());
        if (dto.getBirth() != null) user.setBirth(dto.getBirth());
        if (dto.getProfileImage() != null) user.setProfileImage(dto.getProfileImage());

        userRepository.save(user);
    }

    // ✅ 만료된 인증번호 자동 삭제 (1분마다)
    @Scheduled(fixedRate = 60_000)
    public void cleanupExpiredVerificationCodes() {
        verificationCodes.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
