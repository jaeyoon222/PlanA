package com.study.StudyCafe.controller;

import com.study.StudyCafe.dto.NLP.ParsedRequest;
import com.study.StudyCafe.service.KoreanReservationParser;
import com.study.StudyCafe.dto.seat.SeatDto;
import com.study.StudyCafe.repository.*;
import com.study.StudyCafe.entity.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nlu")
@RequiredArgsConstructor
public class NluController {

    private final KoreanReservationParser parser;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final StudyZoneRepository studyZoneRepository;

    @PostMapping("/parse")
    public ParsedRequest parse(@RequestBody Map<String,String> body){
        var pr = parser.parse(body.getOrDefault("text",""));
        pr.validateOrThrow(Duration.ofHours(1));
        return pr;
    }

    @PostMapping("/parse-and-list")
    public Map<String, Object> parseAndList(@RequestBody Map<String, String> body) {
        var pr = parser.parse(body.getOrDefault("text", ""));
        pr.validateOrThrow(Duration.ofHours(1));

        // ✅ zoneId 매핑
        Long zoneId = null;
        if (pr.getBranch() != null) {
            String target = pr.getBranch().replaceAll("점$", "").replaceAll("\\s+", "").toLowerCase();

            zoneId = studyZoneRepository.findAll().stream()
                    .filter(z -> {
                        String zoneName = z.getZoneName();
                        if (zoneName == null) return false;
                        String normalized = zoneName.replaceAll("점$", "").replaceAll("\\s+", "").toLowerCase();
                        return normalized.equals(target);
                    })
                    .map(z -> z.getId())
                    .findFirst()
                    .orElse(null);
        }

        // ✅ ❗존재하지 않는 지점일 경우 에러 응답
        if (zoneId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 지점입니다.");
        }
        // ✅ 좌석 조회
        var seats = seatRepository.findAllByZoneId(Optional.ofNullable(zoneId).orElse(1L));

        // ✅ 만료된 hold 정리
        seats.stream()
                .filter(s -> "hold".equalsIgnoreCase(s.getStatus()) && !s.isHoldActive())
                .forEach(Seat::clearHold);

        var start = pr.getStartDateTime();
        var end = pr.getEndDateTime();

        // ✅ 예약 및 홀딩 좌석 조회
        var reservations = reservationRepository.findOverlapping(
                seats.get(0).getZone().getId(), start, end
        );
        var reservedIds = reservations.stream().map(r -> r.getSeat().getId()).collect(Collectors.toSet());

        var holdingIds = seats.stream()
                .filter(s -> "hold".equalsIgnoreCase(s.getStatus()) && s.isHoldActiveFor(start, end))
                .map(Seat::getId)
                .collect(Collectors.toSet());

        // ✅ 창가 필터
        // ✅ 태그 조건 추출
        Set<String> tags = pr.getSeatTags();
        boolean wantWindow = tags.contains("WINDOW");
        boolean wantOutlet = tags.contains("OUTLET");
        boolean wantQuiet = tags.contains("QUIET");

        var dtos = seats.stream()
                .filter(s -> {
                    boolean include = true;

                    if (wantWindow) {
                        include &= Boolean.TRUE.equals(s.isWindowSide());
                    }
                    if (wantOutlet) {
                        include &= Boolean.TRUE.equals(s.isHasOutlet()); // ✅ Seat 엔티티에 필요
                    }
                    if (wantQuiet) {
                        include &= Boolean.TRUE.equals(s.isQuiet()); // ✅ Seat 엔티티에 필요
                    }

                    return include;
                })
                .map(s -> {
                    SeatDto dto = new SeatDto();
                    dto.setId(s.getId());
                    dto.setSeatName(s.getSeatName());
                    dto.setPosX(s.getPosX());
                    dto.setPosY(s.getPosY());
                    dto.setPrice(s.getPrice());
                    dto.setZoneId(s.getZone().getId());

                    if (reservedIds.contains(s.getId())) dto.setStatus("reserved");
                    else if (holdingIds.contains(s.getId())) dto.setStatus("hold");
                    else dto.setStatus("available");

                    dto.setHoldUntil(s.getHoldExpiresAt());
                    dto.setWindowSide(Boolean.TRUE.equals(s.isWindowSide()));
                    dto.setHasOutlet(Boolean.TRUE.equals(s.isHasOutlet()));
                    dto.setQuiet(Boolean.TRUE.equals(s.isQuiet()));

                    return dto;
                })
                .toList();

        return Map.of("parsed", pr, "seats", dtos);
    }

}
