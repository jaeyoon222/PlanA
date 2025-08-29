package com.study.StudyCafe.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentResponseDto {
    private String merchantUid;
    private int amount;
    private String status;
    private String message;
}
