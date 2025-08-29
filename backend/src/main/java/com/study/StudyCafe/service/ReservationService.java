    // src/main/java/com/study/StudyCafe/service/ReservationService.java
    package com.study.StudyCafe.service;

    import com.study.StudyCafe.config.SeatEventPublisher;
    import com.study.StudyCafe.constant.ReservationStatus;
    import com.study.StudyCafe.dto.seat.SeatEvent;
    import com.study.StudyCafe.entity.Payment;
    import com.study.StudyCafe.entity.Reservation;
    import com.study.StudyCafe.entity.Seat;
    import com.study.StudyCafe.entity.User;
    import com.study.StudyCafe.repository.ReservationRepository;
    import com.study.StudyCafe.repository.SeatRepository;
    import com.study.StudyCafe.repository.UserRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.messaging.simp.SimpMessagingTemplate;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.time.LocalDateTime;
    import java.util.List;

    @Service
    @RequiredArgsConstructor
    public class ReservationService {

        private final ReservationRepository reservationRepository;
        private final SeatRepository seatRepository;
        private final UserRepository userRepository;
        private final SeatEventPublisher seatEventPublisher; // ✅ Redis 발행기

        @Transactional
        public void reserveSeats(List<Long> seatIds, Long userId, LocalDateTime start, LocalDateTime end, Payment payment) {
            if (start == null || end == null || !end.isAfter(start)) {
                throw new IllegalStateException("시작/종료 시간이 올바르지 않습니다.");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            for (Long seatId : seatIds) {
                Seat seat = seatRepository.findByIdForUpdate(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다. id=" + seatId));

                if (Seat.STATUS_HOLD.equals(seat.getStatus()) && !seat.isHoldActive()) {
                    seat.clearHold();
                }

                if (reservationRepository.existsBySeatIdAndTimeOverlap(seatId, start, end)) {
                    throw new IllegalStateException("이미 예약된 시간과 겹치는 좌석이 있습니다. seatId=" + seatId);
                }

                if (Seat.STATUS_HOLD.equals(seat.getStatus())
                        && (seat.getHoldUser() == null || !seat.getHoldUser().getId().equals(userId))) {
                    throw new IllegalStateException("다른 사용자가 홀드 중인 좌석이 있습니다. seatId=" + seatId);
                }

                Reservation reservation = new Reservation();
                reservation.setSeat(seat);
                reservation.setStartTime(start);
                reservation.setEndTime(end);
                reservation.setUser(user);
                reservation.setPayment(payment);
                reservation.setStatus(ReservationStatus.RESERVED);
                reservationRepository.save(reservation);

                seat.setStatus(Seat.STATUS_RESERVED);
                seat.clearHold();
                seatRepository.save(seat);

                seatEventPublisher.publishSeatEvent(new SeatEvent(
                        List.of(seat.getId()),
                        Seat.STATUS_RESERVED,
                        null,
                        userId,
                        "reserve",
                        seat.getZone().getId()
                ));
            }
        }

        @Transactional
        public void cancelReservationsByPayment(Payment payment) {
            List<Reservation> reservations = reservationRepository.findAllByPayment(payment);
            if (reservations.isEmpty()) {
                return;
            }

            for (Reservation reservation : reservations) {
                reservation.setStatus(ReservationStatus.CANCELED);
                reservationRepository.save(reservation);

                Seat seat = reservation.getSeat();
                seat.setStatus(Seat.STATUS_AVAILABLE);
                seat.clearHold();
                seatRepository.save(seat);

                seatEventPublisher.publishSeatEvent(new SeatEvent(
                        List.of(seat.getId()),
                        Seat.STATUS_AVAILABLE,
                        null,
                        reservation.getUser().getId(),
                        "cancel",
                        seat.getZone().getId()
                ));
            }
        }
    }
