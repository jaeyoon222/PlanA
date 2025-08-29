package com.study.StudyCafe.entity;

import com.study.StudyCafe.constant.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    @Email
    private String email;           // 소셜에서 null 가능 → DB 제약 조정 필요 시 nullable=true로 변경 검토

    private String name;
    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    // 소셜
    @Column(length = 20)
    private String provider;        // google|kakao|naver|local

    @Column(length = 100)
    private String providerId;      // 공급자 고유 ID

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    private String password;        // local만 사용, 소셜 null 가능

    private String nickname;

    private LocalDate birth;

    public boolean isSocialUser() {
        return provider != null && !provider.equals("local");
    }

    @OneToMany(mappedBy = "user")
    private List<Reservation> reservations;

    @OneToMany(mappedBy = "user")
    private List<Payment> payments;
}