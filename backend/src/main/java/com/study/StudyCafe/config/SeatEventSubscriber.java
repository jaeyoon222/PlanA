package com.study.StudyCafe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.StudyCafe.dto.seat.SeatEvent;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class SeatEventSubscriber {

    private final ObjectMapper objectMapper;

    public SeatEventSubscriber(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void handleMessage(String message) {
        try {
            SeatEvent event = objectMapper.readValue(message, SeatEvent.class);
            System.out.println("✅ Redis 메시지 수신: " + event);
        } catch (Exception e) {
            System.err.println("❌ 메시지 처리 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

