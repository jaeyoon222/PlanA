package com.study.StudyCafe.dto.payment;

import com.study.StudyCafe.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReservationResponseDto {

    private Long id;
    private String userEmail;
    private String seatName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 💡 엔티티를 DTO로 변환하는 메서드
    public static ReservationResponseDto fromEntity(Reservation reservation) {
        return new ReservationResponseDto(
                reservation.getId(),
                reservation.getUser().getEmail(),
                reservation.getSeat().getSeatName(),
                reservation.getStartTime(),
                reservation.getEndTime()
        );
    }
}
