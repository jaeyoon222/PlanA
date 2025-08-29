package com.study.StudyCafe.controller;

import com.study.StudyCafe.dto.seat.StudyZoneDto;
import com.study.StudyCafe.repository.StudyZoneRepository;
import com.study.StudyCafe.entity.StudyZone;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final StudyZoneRepository studyZoneRepository;

    // ✅ 모든 지역(Zone)을 DTO로 변환해서 반환
    @GetMapping
    public List<StudyZoneDto> getAllZones() {
        return studyZoneRepository.findAll().stream()
                .map(StudyZoneDto::from) // ✅ Entity → DTO 변환
                .toList();
    }

    @GetMapping("/{id}")
    public StudyZoneDto getZoneById(@PathVariable Long id) {
        StudyZone zone = studyZoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 zoneId: " + id));
        return StudyZoneDto.from(zone);
    }
}
