package com.study.StudyCafe.service;

import com.study.StudyCafe.entity.StudyZone;
import com.study.StudyCafe.repository.StudyZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyZoneService {

    private final StudyZoneRepository studyZoneRepository;
    private final SeatService seatService; // ✅ SeatService 주입

    /**
     * Zone(지역)을 생성하고, 해당 지역에 좌석도 자동 생성
     * @param zoneName   지역 이름
     * @param seatCount  생성할 좌석 수
     */
    @Transactional
    public StudyZone createZoneWithSeats(String zoneName, int seatCount) {
        StudyZone zone = new StudyZone();
        zone.setZoneName(zoneName);

        // 1. 지역 저장
        studyZoneRepository.save(zone);

        // 2. 좌석 자동 생성
        seatService.createStudyCafeSeats(zone.getId(), seatCount);

        return zone;
    }
}
