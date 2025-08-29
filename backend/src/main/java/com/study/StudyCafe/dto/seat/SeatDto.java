package com.study.StudyCafe.dto.seat;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeatDto {
    private Long id;
    private String seatName;
    private int posX;
    private int posY;
    private int price;
    private Long zoneId;
    private String status;              // 선택
    private LocalDateTime holdUntil;
    private boolean windowSide;
    private boolean hasOutlet;          // ✅ 추가
    private boolean quiet;
}

