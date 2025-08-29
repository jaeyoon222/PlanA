package com.study.StudyCafe.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserRegisterDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    private String nickname;

    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDate birth;

    private String phone;

    private String address;
    private String profileImage;
}
