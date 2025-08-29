package com.study.StudyCafe.controller;

import com.study.StudyCafe.entity.Reservation;
import com.study.StudyCafe.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QrController {

    private final ReservationRepository reservationRepository;

    @GetMapping("/api/checkin/{token}")
    public ResponseEntity<String> checkin(@PathVariable String token) {
        Optional<Reservation> opt = reservationRepository.findByQrToken(token);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("유효하지 않은 QR입니다.");
        }

        Reservation reservation = opt.get();
        LocalDateTime now = LocalDateTime.now();

        if (reservation.getQrStartTime() != null && now.isBefore(reservation.getQrStartTime())) {
            return ResponseEntity.status(403).body("아직 입장할 수 없습니다.");
        }

        if (now.isAfter(reservation.getEndTime())) {
            return ResponseEntity.status(410).body("예약 시간이 종료되어 입장할 수 없습니다.");
        }

        if (reservation.isUsed()) {
            return ResponseEntity.status(409).body("이미 입장 처리된 예약입니다.");
        }

        reservation.setUsed(true);
        reservationRepository.save(reservation);

        return ResponseEntity.ok("입장 완료! 이용해 주셔서 감사합니다.");
    }
}
