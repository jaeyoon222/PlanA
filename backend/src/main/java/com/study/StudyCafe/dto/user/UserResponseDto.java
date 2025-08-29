package com.study.StudyCafe.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.study.StudyCafe.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;
    private String provider;
    private String profileImage;
    private String role;
    private String address;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.provider = user.getProvider();
        this.profileImage = user.getProfileImage();
        this.role = user.getRole().name();
        this.address = user.getAddress();
        this.birth = user.getBirth();
    }
}

