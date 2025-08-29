package com.study.StudyCafe.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReservationRequestDto {
    private List<String> seatNames;      // 예: ["A1", "A2"]
    private String startTime;            // 예: "2025-08-19T09:00"
    private String endTime;              // 예: "2025-08-19T11:00"
    private String userId;               // 사용자 ID
}
