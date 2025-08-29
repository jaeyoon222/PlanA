package com.study.StudyCafe.dto.reserve;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationResponseDto {
    //예약 결과 응답
    private Long reservationId;
    private String seatNumber;
    private String timeRange;
    private String status;
    private String qrCode;
}
