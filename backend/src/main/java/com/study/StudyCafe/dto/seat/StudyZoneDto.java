package com.study.StudyCafe.dto.seat;

import com.study.StudyCafe.entity.StudyZone;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudyZoneDto {
    private Long id;
    private String zoneName;
    private String description;

    private double latitude;
    private double longitude;

    public static StudyZoneDto from(StudyZone zone) {
        return new StudyZoneDto(
                zone.getId(),
                zone.getZoneName(),
                zone.getDescription(),
                zone.getLatitude(),
                zone.getLongitude()
        );
    }
}
