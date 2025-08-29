package com.study.StudyCafe.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.StudyCafe.dto.seat.SeatEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeatEventPublisher {

    private final RedisTemplate<String, String> redisStringTemplate;
    private final ObjectMapper objectMapper;

    public void publishSeatEvent(SeatEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisStringTemplate.convertAndSend("seat-events", json);
        } catch (JsonProcessingException e) {
            System.err.println("Redis 메시지 직렬화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

