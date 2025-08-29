package com.study.StudyCafe.entity;

import com.study.StudyCafe.constant.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {
    @Id
    private String merchantUid; // ex: mid_123456789

    private String impUid;
    private int amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PAID, CANCELLED 등

    private String seatNames; // 예: "A1, A2"
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalMinutes;

    @ManyToOne
    private User user;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    private List<Reservation> reservations;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @ManyToOne
    private StudyZone studyName;
}

