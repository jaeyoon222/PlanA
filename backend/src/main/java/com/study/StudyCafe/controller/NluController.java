package com.study.StudyCafe.controller;

import com.study.StudyCafe.dto.NLP.ParsedRequest;
import com.study.StudyCafe.service.KoreanReservationParser;
import com.study.StudyCafe.dto.seat.SeatDto;
import com.study.StudyCafe.repository.*;
import com.study.StudyCafe.entity.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

        System.out.println("🧭 브랜치: " + pr.getBranch());
        System.out.println("🎯 매핑된 zoneId: " + zoneId);

        // ✅ 좌석 조회
        var seats = seatRepository.findAllByZoneId(Optional.ofNullable(zoneId).orElse(1L));
        System.out.println("💺 가져온 좌석 수: " + seats.size());

        // ✅ 만료된 hold 정리
        seats.stream()
                .filter(s -> "hold".equalsIgnoreCase(s.getStatus()) && !s.isHoldActive())
                .forEach(Seat::clearHold);

        var start = pr.getStartDateTime();
        var end = pr.getEndDateTime();

        System.out.println("⏰ 예약 시간: " + start + " ~ " + end);

        if (start == null || end == null) {
            System.out.println("❌ 시간 파싱 실패: start 또는 end가 null입니다.");
        }

        // ✅ 예약 및 홀딩 좌석 조회
        var reservations = reservationRepository.findOverlapping(
                seats.get(0).getZone().getId(), start, end
        );
        var reservedIds = reservations.stream().map(r -> r.getSeat().getId()).collect(Collectors.toSet());

        var holdingIds = seats.stream()
                .filter(s -> "hold".equalsIgnoreCase(s.getStatus()) && s.isHoldActiveFor(start, end))
                .map(Seat::getId)
                .collect(Collectors.toSet());

        System.out.println("📌 예약 좌석 수: " + reservedIds.size());
        System.out.println("🔒 홀딩 좌석 수: " + holdingIds.size());

        // ✅ 창가 필터
        // ✅ 태그 조건 추출
        Set<String> tags = pr.getSeatTags();
        boolean wantWindow = tags.contains("WINDOW");
        boolean wantOutlet = tags.contains("OUTLET");
        boolean wantQuiet = tags.contains("QUIET");

        System.out.println("🏷️ 요청 태그: " + tags);
        System.out.println("🌞 창가 필터 적용: " + wantWindow);
        System.out.println("🔌 콘센트 필터 적용: " + wantOutlet);
        System.out.println("🤫 조용한 필터 적용: " + wantQuiet);

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

                    System.out.println("🪑 " + s.getSeatName() +
                            " | 창가: " + s.isWindowSide() +
                            " | 콘센트: " + s.isHasOutlet() +
                            " | 조용한: " + s.isQuiet() +
                            " | 포함됨: " + include);

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

                    System.out.println("➡️ 최종 포함 좌석: " + s.getSeatName() + " | 상태: " + dto.getStatus());
                    return dto;
                })
                .toList();


        System.out.println("✅ 최종 응답 좌석 수: " + dtos.size());

        return Map.of("parsed", pr, "seats", dtos);
    }

}
