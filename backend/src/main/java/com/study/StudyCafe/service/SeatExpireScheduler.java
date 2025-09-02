// SeatExpireScheduler.java
package com.study.StudyCafe.service;

import com.study.StudyCafe.entity.Seat;
import com.study.StudyCafe.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SeatExpireScheduler {

    private final SeatRepository seatRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelay = 50000)
    @Transactional
    public void sweepExpiredHolds() {
        List<Seat> expired = seatRepository.findExpiredHolds(LocalDateTime.now());
        if (expired.isEmpty()) return;

        Map<Long, List<Long>> zoneToSeatIds = new HashMap<>();
        for (Seat s : expired) {
            s.clearHold(); // status=available, holdUser=null, holdExpiresAt=null
            Long zoneId = s.getZone().getId();
            zoneToSeatIds.computeIfAbsent(zoneId, k -> new ArrayList<>()).add(s.getId());
        }
        seatRepository.saveAll(expired);

        // 프론트가 기대하는 형식으로 존별 브로드캐스트
        zoneToSeatIds.forEach((zoneId, ids) -> {
            Map<String, Object> ev = Map.of(
                    "seatIds", ids,
                    "status", "available"
            );
            messagingTemplate.convertAndSend("/topic/seats/" + zoneId, ev);
        });
    }
}
