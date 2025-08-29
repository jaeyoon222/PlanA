package com.study.StudyCafe.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateDto {
    private String name;
    private String nickname;
    private String phone;
    private String address;
    private String profileImage;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;

    // 본인 인증용 현재 비밀번호
    private String currentPassword;

    // 변경할 새 비밀번호
    private String newPassword;

    private String verificationCode;
}
