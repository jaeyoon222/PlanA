// src/main/java/com/study/StudyCafe/dto/seat/SeatEvent.java
package com.study.StudyCafe.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor; import lombok.Data; import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatEvent {
    private List<Long> seatIds;
    private String status;

    private LocalDateTime holdUntil;  // ✅ JSON 필드 이름과 동일하게

    private Long byUserId;
    private String eventType;
    private Long zoneId;
}
