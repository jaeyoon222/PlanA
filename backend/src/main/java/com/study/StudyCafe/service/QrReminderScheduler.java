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

            if (minutesUntilStart <= 30) {
                String phone = res.getUser().getPhone();

                try {
                    // ✅ QR 토큰 없으면 생성
                    if (res.getQrToken() == null) {
                        String token = UUID.randomUUID().toString();
                        res.setQrToken(token);
                    }

                    reservationRepository.save(res); // 저장

                    String token = res.getQrToken();

                    // ✅ QR에 인코딩될 진짜 주소 (스캔 결과 처리용)
                    String checkinUrl = "https://43.201.178.143/checkin-result/" + token;

                    // ✅ QR 이미지 파일 생성
                    qrGenerator.generateQr(checkinUrl, token);

                    // ✅ 사용자가 눌렀을 때 QR을 눈으로 볼 수 있는 페이지
                    String viewQrUrl = "https://43.201.178.143:3000/qrcode/" + token;

                    log.info("[QR 전송 대상] 예약ID={}, 전화번호={}, 보기용 URL={}", res.getId(), phone, viewQrUrl);

                    smsService.sendSMS(
                            phone,
                            "[스터디카페] 예약 30분 전입니다. 아래 QR을 촬영해 입장하세요:\n" + viewQrUrl
                    );

                    log.info("[QR 문자 전송 완료] 예약ID={}, 전화번호={}", res.getId(), phone);

                    res.setNotified(true);
                    reservationRepository.save(res);
                }
                catch (WriterException | IOException e) {
                    log.error("[QR 생성 실패] 예약ID={}, 오류={}", res.getId(), e.getMessage());
                } catch (Exception e) {
                    log.error("[문자 전송 실패] 예약ID={}, 오류={}", res.getId(), e.getMessage());
                }
            }
        }

    }
    }