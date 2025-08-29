package com.study.StudyCafe.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentDetailDto {
    private String merchantUid;
    private String impUid;
    private int amount;
    private String seatNames;
    private String startTime;
    private String endTime;
    private int totalMinutes;
    private String userId;
    private String status;
    private String zoneName;
    private Long createdAt;
}
