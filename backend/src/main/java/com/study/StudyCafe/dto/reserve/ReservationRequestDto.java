package com.study.StudyCafe.dto.reserve;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReservationRequestDto {
    private List<Long> seatIds;           // 예약할 좌석 ID 목록
    private LocalDateTime startTime;      // 시작 시간
    private LocalDateTime endTime;        // 종료 시간
}
