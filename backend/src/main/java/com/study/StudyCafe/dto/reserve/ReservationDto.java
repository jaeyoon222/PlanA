package com.study.StudyCafe.dto.reserve;

import com.study.StudyCafe.constant.ReservationStatus;

import com.study.StudyCafe.entity.Reservation;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@ToString
public class ReservationDto {

    private Long reservationId;
    private String userId;

    private String seatNumber;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    private ReservationStatus status;

    private String qrCode;
    private String payMethod;
    private Integer price;

    private static ModelMapper modelMapper = new ModelMapper();

    public Reservation createReservation() {
        return modelMapper.map(this, Reservation.class);
    }

    public static ReservationDto of(Reservation reservation) {
        return modelMapper.map(reservation, ReservationDto.class);
    }
}
