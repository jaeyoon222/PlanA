package com.study.StudyCafe.repository;

import com.study.StudyCafe.constant.ReservationStatus;
import com.study.StudyCafe.entity.Payment;
import com.study.StudyCafe.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r WHERE r.seat.zone.id = :zoneId AND DATE(r.startTime) = CURRENT_DATE")
    List<Reservation> findByZoneIdAndToday(Long zoneId);

    @Query("""
    SELECT COUNT(r) > 0
    FROM Reservation r
    WHERE r.seat.id = :seatId
      AND r.startTime < :endTime
      AND r.endTime > :startTime
      AND r.status = com.study.StudyCafe.constant.ReservationStatus.RESERVED
""")
    boolean existsBySeatIdAndTimeOverlap(Long seatId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("""
    SELECT r FROM Reservation r
    WHERE r.seat.zone.id = :zoneId
      AND r.startTime < :endTime
      AND r.endTime > :startTime
      AND r.status = com.study.StudyCafe.constant.ReservationStatus.RESERVED
""")
    List<Reservation> findOverlapping(Long zoneId, LocalDateTime startTime, LocalDateTime endTime);

    Optional<Reservation> findByPayment(Payment payment);

    List<Reservation> findAllByPayment(Payment payment);

    List<Reservation> findByStartTimeAfterAndIsNotifiedFalseAndStatus(LocalDateTime now, ReservationStatus status);

    Optional<Reservation> findByQrToken(String qrToken);


}
