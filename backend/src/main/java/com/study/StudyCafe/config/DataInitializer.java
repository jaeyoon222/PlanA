package com.study.StudyCafe.config;

import com.study.StudyCafe.entity.Seat;
import com.study.StudyCafe.entity.StudyZone;
import com.study.StudyCafe.repository.SeatRepository;
import com.study.StudyCafe.repository.StudyZoneRepository;
import com.study.StudyCafe.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StudyZoneRepository studyZoneRepository;
    private final SeatRepository seatRepository;     // ✅ 좌석 수정용
    private final SeatService seatService;

    @Override
    public void run(String... args) throws Exception {
        if (studyZoneRepository.count() == 0) {
            createZone("강남", "서울 강남구 위치", 20, 37.5112, 127.1317);
            createZone("부평", "인천 부평구 위치", 15, 37.5075, 126.7413);
            createZone("부천", "경기 부천시 위치", 25, 37.5043, 126.7630);

            System.out.println("✅ 지점 및 좌석 초기 데이터 생성 완료!");

            loadSeatTagsFromCsv(); // ✅ 좌석 태그 설정 CSV 로딩
        } else {
            System.out.println("ℹ️ 지점 데이터가 이미 존재합니다. 초기화 생략.");
        }
    }

    private void createZone(String name, String description, int seatCount, double lat, double lng) {
        StudyZone zone = new StudyZone();
        zone.setZoneName(name);
        zone.setDescription(description);
        zone.setLatitude(lat);
        zone.setLongitude(lng);
        studyZoneRepository.save(zone);

        seatService.createStudyCafeSeats(zone.getId(), seatCount);
    }

    // ✅ CSV 파일로부터 좌석 태그 설정
    private void loadSeatTagsFromCsv() {
        try (
                InputStream is = getClass().getClassLoader().getResourceAsStream("seat_tags.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("zoneName")) continue; // 헤더 건너뜀

                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                String zoneName = parts[0].trim();
                String seatName = parts[1].trim();
                boolean window = Boolean.parseBoolean(parts[2].trim());
                boolean outlet = Boolean.parseBoolean(parts[3].trim());
                boolean quiet = Boolean.parseBoolean(parts[4].trim());

                StudyZone zone = studyZoneRepository.findByZoneName(zoneName).orElse(null);
                if (zone == null) {
                    System.err.println("존재하지 않는 지점: " + zoneName);
                    continue;
                }

                Seat seat = seatRepository.findByZoneAndSeatName(zone, seatName).orElse(null);
                if (seat == null) {
                    System.err.println("존재하지 않는 좌석: " + seatName + " in " + zoneName);
                    continue;
                }

                seat.setWindowSide(window);
                seat.setHasOutlet(outlet);
                seat.setQuiet(quiet);
                seatRepository.save(seat);
            }

            System.out.println("✅ seat_tags.csv를 통해 좌석 태그 설정 완료!");

        } catch (Exception e) {
            System.err.println("❌ CSV 파일 로딩 중 오류 발생: " + e.getMessage());
        }
    }
}
