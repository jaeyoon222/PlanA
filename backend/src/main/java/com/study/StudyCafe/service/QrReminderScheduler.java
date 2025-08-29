package com.study.StudyCafe.service;

import com.study.StudyCafe.constant.ReservationStatus;
import com.study.StudyCafe.entity.Reservation;
import com.study.StudyCafe.repository.ReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import com.google.zxing.WriterException;
import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class QrReminderScheduler {

    private final ReservationRepository reservationRepository;
    private final SmsService smsService;
    private final QrGenerator qrGenerator;

    public QrReminderScheduler(ReservationRepository reservationRepository, SmsService smsService, QrGenerator qrGenerator) {
        this.reservationRepository = reservationRepository;
        this.smsService = smsService;
        this.qrGenerator = qrGenerator;
    }

    @Scheduled(fixedRate = 60000)
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> reservations = reservationRepository
                .findByStartTimeAfterAndIsNotifiedFalseAndStatus(now, ReservationStatus.RESERVED);

        log.info("[QR 스케줄러] {} 기준 예약 {}건 조회됨", now, reservations.size());

        for (Reservation res : reservations) {
            long minutesUntilStart = java.time.Duration.between(now, res.getStartTime()).toMinutes();

            if (minutesUntilStart <= 60) {
                String phone = res.getUser().getPhone();

                try {
                    // ✅ QR 토큰 없으면 생성
                    if (res.getQrToken() == null) {
                        String token = UUID.randomUUID().toString();
                        res.setQrToken(token);
                    }

                    reservationRepository.save(res); // 저장 꼭 필요

                    String token = res.getQrToken();
                    String checkinUrl = "https://14.37.8.141/checkin/" + token;
                    String imagePath = qrGenerator.generateQr(checkinUrl, token);
                    String qrImageUrl = "https://14.37.8.141:3000" + imagePath;

                    log.info("[QR 전송 대상] 예약ID={}, 전화번호={}, QR URL={}", res.getId(), phone, qrImageUrl);

                    smsService.sendSMS(
                            phone,
                            "[스터디카페] 예약 30분 전입니다. 아래 QR을 사용해 입장하세요: " + qrImageUrl
                    );

                    log.info("[QR 문자 전송 완료] 예약ID={}, 전화번호={}", res.getId(), phone);

                    res.setNotified(true);
                    reservationRepository.save(res);

                } catch (WriterException | IOException e) {
                    log.error("[QR 생성 실패] 예약ID={}, 오류={}", res.getId(), e.getMessage());
                } catch (Exception e) {
                    log.error("[문자 전송 실패] 예약ID={}, 오류={}", res.getId(), e.getMessage());
                }
            }
        }

    }
    }