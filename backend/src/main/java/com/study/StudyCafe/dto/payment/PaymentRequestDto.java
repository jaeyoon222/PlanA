package com.study.StudyCafe.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PaymentRequestDto {
    private String impUid;
    private String merchantUid;
    private int amount;

    private String userId;
    private List<Long> seatIds;
    private String startTime;
    private String endTime;
    private String zoneName;
}