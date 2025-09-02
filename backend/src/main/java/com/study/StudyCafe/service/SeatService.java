// src/main/java/com/study/StudyCafe/service/SeatService.java
package com.study.StudyCafe.service;

import com.study.StudyCafe.config.SeatEventPublisher; // ✅ 추가
import com.study.StudyCafe.dto.seat.SeatEvent;
import com.study.StudyCafe.entity.Seat;
import com.study.StudyCafe.entity.StudyZone;
import com.study.StudyCafe.entity.User;
import com.study.StudyCafe.repository.ReservationRepository;
import com.study.StudyCafe.repository.SeatRepository;
import com.study.StudyCafe.repository.StudyZoneRepository;
import com.study.StudyCafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final StudyZoneRepository studyZoneRepository;
    private final ReservationRepository reservationRepository;
    private final SeatEventPublisher seatEventPublisher; // ✅ Redis 발행기

    private static final int HOLD_MINUTES = 5;

    @Transactional
    public void holdSeat(Long seatId, Long userId,
                         LocalDateTime startTime, LocalDateTime endTime) {

        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다."));

        if ("hold".equals(seat.getStatus()) && !seat.isHoldActive()) {
            seat.clearHold();
        }

        boolean hasOverlapReservation =
                reservationRepository.existsBySeatIdAndTimeOverlap(seatId, startTime, endTime);
        if (hasOverlapReservation) {
            throw new IllegalStateException("해당 시간대에 이미 예약이 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if ("hold".equals(seat.getStatus())) {
            if (seat.getHoldUser() != null && seat.getHoldUser().getId().equals(userId)) {
                boolean overlapHold = seat.isHoldActiveFor(startTime, endTime);
                if (!overlapHold) {
                    seat.setHoldStartTime(startTime);
                    seat.setHoldEndTime(endTime);
                }
                seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES));
            } else {
                if (seat.getHoldUser() != null && seat.isHoldActiveFor(startTime, endTime)) {
                    throw new IllegalStateException("다른 사용자가 해당 시간대를 홀드 중입니다.");
                }
                seat.setStatus("hold");
                seat.setHoldUser(user);
                seat.setHoldStartTime(startTime);
                seat.setHoldEndTime(endTime);
                seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES));
            }
        } else {
            seat.setStatus("hold");
            seat.setHoldUser(user);
            seat.setHoldStartTime(startTime);
            seat.setHoldEndTime(endTime);
            seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES));
        }

        seatRepository.save(seat);

        // ✅ Redis로 이벤트 발행 (WebSocket은 Subscriber가 처리함)
        SeatEvent event = new SeatEvent(
                List.of(seat.getId()),
                "hold",
                seat.getHoldExpiresAt(),
                userId,
                "hold",
                seat.getZone().getId() // ✅ 반드시 포함
        );
        seatEventPublisher.publishSeatEvent(event);
    }

    @Transactional
    public void releaseHold(Long seatId, Long userId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다."));

        if ("hold".equals(seat.getStatus()) && !seat.isHoldActive()) {
            seat.clearHold();
        }
        if (!"hold".equals(seat.getStatus())) return;

        if (seat.getHoldUser() == null || !seat.getHoldUser().getId().equals(userId)) {
            throw new IllegalStateException("본인이 홀드한 좌석만 취소할 수 있습니다.");
        }

        seat.clearHold();
        seatRepository.save(seat);

        // ✅ Redis로 발행
        SeatEvent event = new SeatEvent(
                List.of(seat.getId()),
                "available",
                null,
                userId,
                "release",
                seat.getZone().getId()
        );
        seatEventPublisher.publishSeatEvent(event);
    }

    @Transactional
    public void createStudyCafeSeats(Long zoneId, int seatCount) {
        if (zoneId == null) throw new IllegalArgumentException("zoneId는 필수입니다.");
        if (seatCount <= 0) throw new IllegalArgumentException("seatCount는 1 이상이어야 합니다.");

        StudyZone zone = studyZoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Zone입니다. id=" + zoneId));

        List<Seat> seats = new ArrayList<>(seatCount);

        int cols = 5;
        for (int i = 0; i < seatCount; i++) {
            Seat seat = new Seat();
            seat.setZone(zone);
            seat.setSeatName(String.valueOf(i + 1));
            seat.setStatus("available");
            seat.setPosX((i % cols) * 10);
            seat.setPosY((i / cols) * 10);
            seats.add(seat);
        }

        seatRepository.saveAll(seats);
    }
}
