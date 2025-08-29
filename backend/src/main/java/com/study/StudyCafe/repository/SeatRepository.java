// src/main/java/com/study/StudyCafe/repository/SeatRepository.java
package com.study.StudyCafe.repository;

import com.study.StudyCafe.entity.Seat;
import com.study.StudyCafe.entity.StudyZone;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.*;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findAllByZoneId(Long zoneId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :id")
    Optional<Seat> findByIdForUpdate(@Param("id") Long id);

    @Query("""
       select s from Seat s
       join fetch s.zone z
       where s.status = 'hold' and s.holdExpiresAt < :now
       """)
    List<Seat> findExpiredHolds(@Param("now") LocalDateTime now);
    Optional<Seat> findByZoneAndSeatName(StudyZone zone, String seatName);
    @Query("SELECT s.id FROM Seat s WHERE s.seatName IN :seatNames")
    List<Long> findIdsBySeatNames(@Param("seatNames") List<String> seatNames);

    @Query("SELECT s FROM Seat s WHERE s.id IN :ids")
    List<Seat> findAllByIdIn(@Param("ids") List<Long> ids);
}
