// src/main/java/com/study/StudyCafe/controller/ReserveController.java
package com.study.StudyCafe.controller;

import com.study.StudyCafe.entity.Payment;
import com.study.StudyCafe.entity.User;
import com.study.StudyCafe.repository.UserRepository;
import com.study.StudyCafe.service.ReservationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReserveController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@RequestBody ReserveRequest body, Principal principal) {
        Long userId = resolveUserId(principal).orElse(null);
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            reservationService.reserveSeats(body.getSeatIds(), userId, body.getStartTime(), body.getEndTime(), body.getPayment());
            return ResponseEntity.ok("예약 완료");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    private Optional<Long> resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) return Optional.empty();
        String sub = principal.getName();
        Optional<User> userOpt = sub.contains("@")
                ? userRepository.findByEmail(sub)
                : findByProviderSubject(sub);
        return userOpt.map(User::getId);
    }

    private Optional<User> findByProviderSubject(String sub) {
        int idx = sub.indexOf(':');
        if (idx < 0) return Optional.empty();
        String provider = sub.substring(0, idx);
        String providerId = sub.substring(idx + 1);
        return userRepository.findByProviderAndProviderId(provider, providerId);
    }

    @Data
    public static class ReserveRequest {
        private List<Long> seatIds;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Payment payment;
    }
}
