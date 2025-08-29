// src/main/java/com/study/StudyCafe/entity/Seat.java
package com.study.StudyCafe.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.ToString;

import java.time.*;

@Entity
@Table(
        name = "seat",
        indexes = {
                @Index(name = "idx_seat_zone", columnList = "zone_id"),
                @Index(name = "idx_seat_status", columnList = "status"),
                @Index(name = "idx_seat_hold_expires", columnList = "hold_expires_at")
        }
)
@Getter @Setter @ToString
public class Seat {
    // 상태 문자열 상수(컨트롤러/서비스에서 오타 방지)
    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_HOLD      = "hold";
    public static final String STATUS_RESERVED  = "reserved";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private StudyZone zone;

    private String seatName;
    private int posX;
    private int posY;
    private int price;

    @Column(nullable = false, length = 16)
    private String status = STATUS_AVAILABLE;

    @PrePersist
    public void prePersist() {
        if (status == null) status = STATUS_AVAILABLE;
    }

    // ✅ 낙관적 락으로 동시성 보강(선택)
    @Version
    private Long version;

    // ✅ 홀드 메타(재진입/중복 방지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hold_user_id")
    private User holdUser;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "hold_start_time")
    private LocalDateTime holdStartTime;

    @Column(name = "hold_end_time")
    private LocalDateTime holdEndTime;

    // ✅ 필터/추천용 속성들 ------------------------------

    @Column(nullable = false)
    private boolean windowSide = false;
    // 컨트롤러에서 Boolean.TRUE.equals(s.isWindowSide()) 사용 중이라 유지
    public Boolean isWindowSide() { return windowSide; }
    public void setWindowSide(boolean v) { this.windowSide = v; }

    // 콘센트 여부
    @Column(nullable = false)
    private boolean hasOutlet = false;
    // 컨트롤러 예시(getHasOutlet)와 호환되도록 둘 다 제공
    public Boolean getHasOutlet() { return hasOutlet; }
    public Boolean isHasOutlet()  { return hasOutlet; }
    public void setHasOutlet(boolean v) { this.hasOutlet = v; }

    // 조용한 자리 여부
    @Column(nullable = false)
    private boolean quiet = false;
    public Boolean getIsQuiet() { return quiet; }
    public Boolean isQuiet()    { return quiet; }
    public void setQuiet(boolean v) { this.quiet = v; }

    // ✅ 유틸 메서드 ------------------------------------

    /** 현재 시점 기준 활성 홀드 여부 */
    public boolean isHoldActive() {
        return STATUS_HOLD.equalsIgnoreCase(status)
                && holdExpiresAt != null
                && holdExpiresAt.isAfter(LocalDateTime.now());
    }

    /** 요청 시간대(reqStart~reqEnd)와 겹치는 활성 홀드 여부 */
    public boolean isHoldActiveFor(LocalDateTime reqStart, LocalDateTime reqEnd) {
        return isHoldActive()
                && holdStartTime != null && holdEndTime != null
                && holdStartTime.isBefore(reqEnd)
                && holdEndTime.isAfter(reqStart);
    }

    /** 시간 구간 겹침 체크(예약/홀드 공통 로직에 활용) */
    public static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd,
                                   LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    /** 홀드 설정 편의 메서드 */
    public void markHold(User user, LocalDateTime reqStart, LocalDateTime reqEnd, Duration ttl) {
        this.status = STATUS_HOLD;
        this.holdUser = user;
        this.holdStartTime = reqStart;
        this.holdEndTime = reqEnd;
        this.holdExpiresAt = LocalDateTime.now().plus(ttl);
    }

    /** 홀드 해제 */
    public void clearHold() {
        this.holdUser = null;
        this.holdExpiresAt = null;
        this.holdStartTime = null;
        this.holdEndTime = null;
        if ("hold".equals(this.status)) {
            this.status = STATUS_AVAILABLE;
        }
    }
}
