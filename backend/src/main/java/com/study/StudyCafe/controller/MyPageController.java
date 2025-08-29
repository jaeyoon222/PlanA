package com.study.StudyCafe.controller;

import com.study.StudyCafe.dto.user.UserUpdateDto;
import com.study.StudyCafe.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;

    // ✅ 환경 변수 또는 설정에서 주입
    @Value("${file.upload.path}")
    private String uploadDir;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        System.out.println("로그아웃");
        return ResponseEntity.ok("로그아웃 성공");
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        System.out.println("🛠️ [uploadFile] 업로드 요청 도착");
        System.out.println("🔎 파일 객체: " + file);
        System.out.println("🔎 Content-Type: " + file.getContentType());
        System.out.println("🔎 파일 크기: " + file.getSize());
        System.out.println("🔎 원본 파일명: " + file.getOriginalFilename());

        if (file.isEmpty()) {
            System.out.println("🚫 업로드된 파일이 비어 있습니다.");
            return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
        }

        try {
            String originalName = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalName;
            Path uploadPath = Paths.get(uploadDir);

            System.out.println("📁 설정된 uploadDir: " + uploadDir);
            System.out.println("📁 uploadPath 절대경로: " + uploadPath.toAbsolutePath());

            // 업로드 디렉토리 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("📂 업로드 디렉토리가 없어 새로 생성함.");
            }

            // 파일 저장
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            System.out.println("✅ 파일 저장 완료!");
            System.out.println("📄 저장된 파일명: " + fileName);
            System.out.println("📍 실제 저장 위치: " + filePath.toAbsolutePath());

            String fileUrl = "/uploads/" + fileName;
            System.out.println("🌐 클라이언트에 전달될 URL: " + fileUrl);

            return ResponseEntity.ok(Map.of("url", fileUrl));

        } catch (Exception e) {
            System.out.println("❌ 파일 저장 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("업로드 실패: " + e.getMessage());
        }
    }


    @PutMapping("/user")
    public ResponseEntity<?> updateUser(
            @RequestBody @Valid UserUpdateDto dto,
            @AuthenticationPrincipal String email
    ) {
        if (email == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        userService.updateUserByEmail(email, dto);
        return ResponseEntity.ok(Map.of("message", "정보 수정 완료"));
    }
}
