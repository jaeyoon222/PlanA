// src/main/java/com/study/StudyCafe/controller/SeatController.java
package com.study.StudyCafe.controller;

import com.study.StudyCafe.dto.seat.SeatDto;
import com.study.StudyCafe.entity.Seat;
import com.study.StudyCafe.entity.User;
import com.study.StudyCafe.repository.ReservationRepository;
import com.study.StudyCafe.repository.SeatRepository;
import com.study.StudyCafe.repository.UserRepository;
import com.study.StudyCafe.service.SeatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeatController {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final SeatService seatService;

    @GetMapping("/seats")
    public ResponseEntity<?> getSeats(
            @RequestParam Long zoneId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String windowSide,
            @RequestParam(required = false) String outlet,
            @RequestParam(required = false) String quiet,
            Principal principal
    ) {
        try {
            if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
                // 유효하지 않은 시간 → 빈 좌석 목록으로 응답
                Map<String, Object> emptyBody = new HashMap<>();
                emptyBody.put("seats", List.of());
                emptyBody.put("reservedSeatIds", Set.of());
                emptyBody.put("holdingSeatIds", Set.of());
                emptyBody.put("holdingByMeSeatIds", Set.of());

                Map<String, Object> filters = new HashMap<>();
                filters.put("zoneId", zoneId);
                filters.put("windowSide", parseBoolean(windowSide));
                filters.put("outlet", parseBoolean(outlet));
                filters.put("quiet", parseBoolean(quiet));
                filters.put("startTime", startTime != null ? startTime.toString() : "null");
                filters.put("endTime", endTime != null ? endTime.toString() : "null");

                emptyBody.put("filters", filters);
                return ResponseEntity.ok(emptyBody);
            }

            Boolean isWindowSide = parseBoolean(windowSide);
            Boolean isOutlet = parseBoolean(outlet);
            Boolean isQuiet = parseBoolean(quiet);

            var seats = Optional.ofNullable(seatRepository.findAllByZoneId(zoneId)).orElse(List.of());

            if (seats.isEmpty()) {
                Map<String, Object> emptyBody = new HashMap<>();
                emptyBody.put("seats", List.of());
                emptyBody.put("reservedSeatIds", Set.of());
                emptyBody.put("holdingSeatIds", Set.of());
                emptyBody.put("holdingByMeSeatIds", Set.of());
                Map<String, Object> filters = new HashMap<>();
                filters.put("zoneId", zoneId);
                filters.put("windowSide", isWindowSide);
                filters.put("outlet", isOutlet);
                filters.put("quiet", isQuiet);
                filters.put("startTime", startTime.toString());
                filters.put("endTime", endTime.toString());
                emptyBody.put("filters", filters);
                return ResponseEntity.ok(emptyBody);
            }

            seats.stream()
                    .filter(s -> "hold".equalsIgnoreCase(s.getStatus()) && !s.isHoldActive())
                    .forEach(Seat::clearHold);

            var reservations = reservationRepository.findOverlapping(zoneId, startTime, endTime);
            var reservedSeatIds = reservations.stream()
                    .map(r -> Optional.ofNullable(r.getSeat()).map(Seat::getId).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            var holdingSeatIds = seats.stream()
                    .filter(s -> "hold".equalsIgnoreCase(s.getStatus()) && s.isHoldActiveFor(startTime, endTime))
                    .map(Seat::getId)
                    .collect(Collectors.toSet());

            Long me = resolveUserId(principal).orElse(null);
            var holdingByMeSeatIds = seats.stream()
                    .filter(s -> "hold".equalsIgnoreCase(s.getStatus()) && s.isHoldActiveFor(startTime, endTime))
                    .filter(s -> me != null && s.getHoldUser() != null && s.getHoldUser().getId().equals(me))
                    .map(Seat::getId)
                    .collect(Collectors.toSet());

            Stream<Seat> stream = seats.stream();
            if (isWindowSide != null) stream = stream.filter(s -> Objects.equals(Boolean.TRUE.equals(s.isWindowSide()), isWindowSide));
            if (isOutlet != null)     stream = stream.filter(s -> Objects.equals(Boolean.TRUE.equals(s.getHasOutlet()), isOutlet));
            if (isQuiet != null)      stream = stream.filter(s -> Objects.equals(Boolean.TRUE.equals(s.getIsQuiet()), isQuiet));

            var filteredSeats = stream.toList();

            var seatDtos = filteredSeats.stream().map(s -> {
                String status = "available";
                if (reservedSeatIds.contains(s.getId())) status = "reserved";
                else if (holdingSeatIds.contains(s.getId())) status = "hold";

                var dto = new SeatDto();
                dto.setId(s.getId());
                dto.setSeatName(s.getSeatName());
                dto.setPosX(s.getPosX());
                dto.setPosY(s.getPosY());
                dto.setZoneId(s.getZone() != null ? s.getZone().getId() : null);
                dto.setStatus(status);
                dto.setHoldUntil(s.getHoldExpiresAt());
                dto.setWindowSide(Boolean.TRUE.equals(s.isWindowSide()));
                dto.setHasOutlet(Boolean.TRUE.equals(s.getHasOutlet()));
                dto.setQuiet(Boolean.TRUE.equals(s.getIsQuiet()));
                return dto;
            }).toList();

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> filters = new HashMap<>();
            filters.put("zoneId", zoneId);
            filters.put("windowSide", isWindowSide);
            filters.put("outlet", isOutlet);
            filters.put("quiet", isQuiet);
            filters.put("startTime", startTime.toString());
            filters.put("endTime", endTime.toString());

            body.put("filters", filters);
            body.put("seats", seatDtos);
            body.put("reservedSeatIds", reservedSeatIds);
            body.put("holdingSeatIds", holdingSeatIds);
            body.put("holdingByMeSeatIds", holdingByMeSeatIds);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
        }
    }
    @PostMapping("/seats/hold")
    public ResponseEntity<?> holdSeat(@RequestBody HoldSeatRequest payload, Principal principal) {
        if (payload == null || payload.getSeatId() == null) {
            return ResponseEntity.badRequest().body("seatId는 필수입니다.");
        }
        if (payload.getStartTime() == null || payload.getEndTime() == null) {
            return ResponseEntity.badRequest().body("startTime과 endTime은 필수입니다.");
        }

        Long userId = resolveUserId(principal).orElse(null);
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            seatService.holdSeat(payload.getSeatId(), userId, payload.getStartTime(), payload.getEndTime());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/seats/release")
    public ResponseEntity<?> releaseHold(@RequestBody HoldSeatRequest payload, Principal principal) {
        if (payload == null || payload.getSeatId() == null) {
            return ResponseEntity.badRequest().body("seatId는 필수입니다.");
        }
        Long userId = resolveUserId(principal).orElse(null);
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            seatService.releaseHold(payload.getSeatId(), userId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null) return null;
        return switch (value.toLowerCase()) {
            case "true", "1", "yes", "있음", "창가", "조용" -> true;
            case "false", "0", "no", "없음", "시끄러움" -> false;
            default -> null;
        };
    }

    private Optional<Long> resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) return Optional.empty();
        String sub = principal.getName();

        if (sub.contains("@")) {
            return userRepository.findByEmail(sub).map(User::getId);
        }

        int idx = sub.indexOf(':');
        if (idx > 0) {
            String provider = sub.substring(0, idx);
            String providerId = sub.substring(idx + 1);
            return userRepository.findByProviderAndProviderId(provider, providerId).map(User::getId);
        }

        try {
            long id = Long.parseLong(sub);
            return Optional.of(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Data
    public static class HoldSeatRequest {
        private Long seatId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime startTime;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endTime;
    }
}

