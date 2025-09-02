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
    private String generateShortToken(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomIndex = (int) (Math.random() * chars.length());
            token.append(chars.charAt(randomIndex));
        }
        return token.toString();
    }

    @Scheduled(fixedRate = 60000)
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> reservations = reservationRepository
                .findByStartTimeAfterAndIsNotifiedFalseAndStatus(now, ReservationStatus.RESERVED);

        log.info("[QR 스케줄러] {} 기준 예약 {}건 조회됨", now, reservations.size());

        for (Reservation res : reservations) {
            long minutesUntilStart = java.time.Duration.between(now, res.getStartTime()).toMinutes();

            log.info("🟡 예약ID={} | 시작: {} | 현재: {} | 남은: {}분", res.getId(), res.getStartTime(), now, minutesUntilStart);

            if (minutesUntilStart <= 30) {
                log.info("🟢 문자 전송 조건 만족 - 예약ID={}", res.getId());
                String phone = res.getUser().getPhone();

                try {
                    if (res.getQrToken() == null) {
                        String token = generateShortToken(10); // 10자 토큰 생성
                        res.setQrToken(token);
                    }

                    reservationRepository.save(res);

                    String token = res.getQrToken();
                    String checkinUrl = "http://43.201.178.143/checkin-result/" + token;

                    // ✅ QR 이미지를 base64로 생성
                    String base64Qr = qrGenerator.generateQrBase64(checkinUrl);

                    // ✅ 문자에 포함할 URL (뷰어 페이지)
                    String viewQrUrl = "http://43.201.178.143:3000/qrcode/" + token;

                    log.info("[QR 전송 대상] 예약ID={}, 전화번호={}, 보기용 URL={}", res.getId(), phone, viewQrUrl);

                    smsService.sendSMS(
                            phone,
                            "[PlanA] 예약 30분 전입니다. QR 확인: " + viewQrUrl
                    );

                    log.info("[QR 문자 전송 완료] 예약ID={}, 전화번호={}", res.getId(), phone);

                    res.setNotified(true);
                    reservationRepository.save(res);
                } catch (WriterException e) {
                    log.error("[QR 생성 실패] 예약ID={}, 오류={}", res.getId(), e.getMessage(), e);
                } catch (Exception e) {
                    log.error("[문자 전송 실패] 예약ID={}, 오류={}", res.getId(), e.getMessage(), e);
                }
            }
        }
    }
}
