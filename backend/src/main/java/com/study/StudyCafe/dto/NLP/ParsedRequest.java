package com.study.StudyCafe.dto.NLP;

import lombok.*;
import java.time.*;
import java.util.*;

@Getter @Setter @Builder(toBuilder = true)
@NoArgsConstructor @AllArgsConstructor
public class ParsedRequest {
    @Builder.Default private ZoneId zoneId = ZoneId.of("Asia/Seoul");

    private LocalDate date;        // 2025-08-14
    private LocalTime startTime;   // 14:00
    private LocalTime endTime;     // 16:00

    private Integer partySize;     // 1,2,4...
    @Builder.Default private Set<String> seatTags = new LinkedHashSet<>(); // ["WINDOW",...]

    private String branch;         // 지점명(선택)
    private String raw;            // 원문

    public LocalDateTime getStartDateTime() {
        if (date == null || startTime == null) return null;
        return LocalDateTime.of(date, startTime);
    }
    public LocalDateTime getEndDateTime() {
        if (date == null || endTime == null) return null;
        return LocalDateTime.of(date, endTime);
    }
    public void validateOrThrow(Duration defaultDuration) {
        if (date == null) throw new IllegalArgumentException("date 없음");
        if (startTime == null && endTime == null) throw new IllegalArgumentException("시간 없음");
        if (startTime != null && endTime == null) endTime = startTime.plus(defaultDuration);
        if (startTime == null && endTime != null) startTime = endTime.minus(defaultDuration);
        if (!getEndDateTime().isAfter(getStartDateTime())) throw new IllegalArgumentException("종료>시작 필요");
        if (partySize != null && partySize < 1) throw new IllegalArgumentException("partySize 오류");
    }
}
