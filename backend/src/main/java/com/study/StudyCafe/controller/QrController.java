package com.study.StudyCafe.controller;

import com.google.zxing.WriterException;
import com.study.StudyCafe.repository.ReservationRepository;
import com.study.StudyCafe.service.QrGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QrController {

    private final ReservationRepository reservationRepository;
    private final QrGenerator qrGenerator;

    // ✅ QR 코드 이미지 (Base64) 생성 API
    @GetMapping("/api/qr/{token}")
    public ResponseEntity<String> generateQr(@PathVariable String token) {
        try {
            String fullUrl = "http://43.201.178.143:3000/qrcode/" + token; // ✅ URL 전체로 변경
            String base64 = qrGenerator.generateQrBase64(fullUrl);
            return ResponseEntity.ok(base64);
        } catch (WriterException e) {
            log.error("QR 생성 실패", e);
            return ResponseEntity.internalServerError().body("QR 생성 실패");
        }
    }

    // ✅ QR 체크인 API
    @GetMapping("/api/checkin/{token}")
    public ResponseEntity<Map<String, String>> checkin(@PathVariable String token) {
        var opt = reservationRepository.findByQrToken(token);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "유효하지 않은 QR 코드입니다."));
        }

        var reservation = opt.get();
        var now = LocalDateTime.now();

        var qrStartTime = reservation.getQrStartTime() != null
                ? reservation.getQrStartTime()
                : reservation.getStartTime().minusMinutes(10);

        if (now.isBefore(qrStartTime)) {
            return ResponseEntity.status(403).body(Map.of("message", "아직 입장 가능한 시간이 아닙니다."));
        }

        if (now.isAfter(reservation.getEndTime().plusMinutes(5))) {
            return ResponseEntity.status(410).body(Map.of("message", "예약 시간이 종료되어 입장할 수 없습니다."));
        }

        if (reservation.isUsed()) {
            return ResponseEntity.status(409).body(Map.of("message", "이미 입장 처리된 예약입니다."));
        }

        reservation.setUsed(true);
        reservationRepository.save(reservation);

        return ResponseEntity.ok(Map.of("message", "입장 완료! 이용해 주셔서 감사합니다."));
    }
}
