package com.study.StudyCafe.entity;

import com.study.StudyCafe.constant.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@ToString
public class Reservation {
    @Id
    @GeneratedValue
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalMinutes;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status; // RESERVED, CANCELLED 등

    @ManyToOne
    private Seat seat;

    @ManyToOne
    private User user;

    @ManyToOne
    private Payment payment; // 결제와 연결 (nullable 아님)

    @Column(nullable = false)
    private boolean isNotified = false;

    private String qrToken; // ✅ QR에 들어갈 고유 토큰 (UUID 등)

    private boolean isUsed = false; // ✅ 이미 입장했는지 여부

    private LocalDateTime qrStartTime; // ✅ 유효기간 설정용
}
