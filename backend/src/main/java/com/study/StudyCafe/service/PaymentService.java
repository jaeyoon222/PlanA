package com.study.StudyCafe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.StudyCafe.constant.PaymentStatus;
import com.study.StudyCafe.dto.payment.PaymentDetailDto;
import com.study.StudyCafe.dto.payment.PaymentRequestDto;
import com.study.StudyCafe.entity.Payment;
import com.study.StudyCafe.entity.Seat;
import com.study.StudyCafe.entity.StudyZone;
import com.study.StudyCafe.entity.User;
import com.study.StudyCafe.repository.PaymentRepository;
import com.study.StudyCafe.repository.SeatRepository;
import com.study.StudyCafe.repository.StudyZoneRepository;
import com.study.StudyCafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final SeatRepository seatRepository;
    private final StudyZoneRepository studyZoneRepository;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final int PRICE_PER_MINUTE = 10; // 분당 단가 (예: 2원/분)

    @Transactional
    public boolean verifyAndReserve(PaymentRequestDto dto) {
        try {
            String impUid = dto.getImpUid();
            String merchantUid = dto.getMerchantUid();
            StudyZone studyZone = studyZoneRepository.findByZoneName(dto.getZoneName())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 zoneName입니다: " + dto.getZoneName()));

            User user = userRepository.findByEmail(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("이메일에 해당하는 사용자를 찾을 수 없습니다."));

            // 1. 아임포트 토큰 발급
            String accessToken = requestAccessToken();

            // 2. 결제 내역 조회
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.iamport.kr/payments/" + impUid))
                    .header("Authorization", accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = new ObjectMapper().readTree(response.body());

            boolean success = root.path("response").path("status").asText().equals("paid");
            int paidAmount = root.path("response").path("amount").asInt();


            if (!success) {
                log.warn("❌ 결제 상태가 'paid'가 아님 - impUid: {}", impUid);
                return false;
            }

            // 3. 금액 검증
            LocalDateTime start = LocalDateTime.parse(dto.getStartTime());
            LocalDateTime end = LocalDateTime.parse(dto.getEndTime());
            long minutes = Duration.between(start, end).toMinutes();

            int expectedAmount = (int) (dto.getSeatIds().size() * minutes * PRICE_PER_MINUTE);

            if (Math.abs(paidAmount - expectedAmount) > 10) {
                log.error("💰 금액 불일치. 예상: {}, 실제: {}", expectedAmount, paidAmount);
                return false;
            }

            List<Long> seatIds = dto.getSeatIds(); // 👈 seatIds 직접 사용

            // 4. seatNames 가져오기 (저장용으로만 사용)
            List<Seat> seats = seatRepository.findAllById(seatIds);
            String seatNamesStr = seats.stream().map(Seat::getSeatName).toList().toString();

            // 5. 결제 정보 저장
            Payment payment = new Payment();
            payment.setImpUid(impUid);
            payment.setMerchantUid(merchantUid);
            payment.setAmount(paidAmount);
            payment.setUser(user);
            payment.setStatus(PaymentStatus.PAID);
            payment.setStartTime(start);
            payment.setEndTime(end);
            payment.setSeatNames(seatNamesStr); // 👈 단순 저장용
            payment.setTotalMinutes((int) minutes);
            payment.setStudyName(studyZone);
            paymentRepository.save(payment);
            paymentRepository.flush();

            // 6. 예약 처리
            reservationService.reserveSeats(seatIds, user.getId(), start, end, payment);

            return true;

        } catch (Exception e) {
            log.error("❌ 결제 검증 및 예약 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    private String requestAccessToken() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String body = "imp_key=7251445755405710&imp_secret=uZbHJBrDF307TXxASqqljRkXokcOlmelhlbTUGlx1zQ87haLvH5i1YhOxHmQM2wGVGuNbdQ2rJdgREyx"; // 실제 키로 교체

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.iamport.kr/users/getToken"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response.body());

        return jsonNode.path("response").path("access_token").asText();
    }
    public PaymentDetailDto getPaymentDetail(String merchantUid) {
        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다."));

        return convertToDto(payment);
    }
    public List<PaymentDetailDto> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .toList();
    }
    @Transactional
    public boolean cancelPayment(String impUid, String reason) {
        try {
            String accessToken = requestAccessToken();

            Map<String, Object> body = new HashMap<>();
            body.put("imp_uid", impUid);
            body.put("reason", reason);

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.iamport.kr/payments/cancel"))
                    .header("Authorization", accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());

            boolean isCancelled = root.path("response").path("status").asText().equalsIgnoreCase("cancelled");

            if (isCancelled) {
                Payment payment = paymentRepository.findByImpUid(impUid)
                        .orElseThrow(() -> new RuntimeException("결제 내역 없음"));
                payment.setStatus(PaymentStatus.CANCEL);
                paymentRepository.save(payment);
            }

            return isCancelled;

        } catch (Exception e) {
            log.error("❌ 결제 취소 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    private PaymentDetailDto convertToDto(Payment payment) {
        PaymentDetailDto dto = new PaymentDetailDto();
        dto.setMerchantUid(payment.getMerchantUid());
        dto.setImpUid(payment.getImpUid());
        dto.setAmount(payment.getAmount());
        dto.setSeatNames(payment.getSeatNames());
        dto.setStartTime(payment.getStartTime() != null ? payment.getStartTime().toString() : null);
        dto.setEndTime(payment.getEndTime() != null ? payment.getEndTime().toString() : null);
        dto.setTotalMinutes(payment.getTotalMinutes());
        dto.setUserId(payment.getUser().getName());
        dto.setStatus(payment.getStatus().name());
        if (payment.getStudyName() != null) dto.setZoneName(payment.getStudyName().getZoneName());

        // ✅ 여기만 핵심: Payment의 createdAt을 epochMillis로 변환해 세팅
        if (payment.getCreatedAt() != null) {
            long millis = payment.getCreatedAt().atZone(KST).toInstant().toEpochMilli();
            dto.setCreatedAt(millis);
        }
        return dto;
    }

    @Transactional
    public boolean cancelPaymentByImpUid(String impUid, String reason) {
        try {
            String accessToken = requestAccessToken(); // 아임포트 토큰 요청

            // 아임포트 환불 API 호출
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.iamport.kr/payments/cancel"))
                    .header("Authorization", accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            String.format("{\"imp_uid\":\"%s\", \"reason\":\"%s\"}", impUid, reason)
                    ))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Payment payment = paymentRepository.findByImpUid(impUid)
                        .orElseThrow(() -> new RuntimeException("결제 정보가 없습니다."));

                // 결제 상태 업데이트
                payment.setStatus(PaymentStatus.CANCEL);
                paymentRepository.save(payment);

                // 예약 정보 상태도 변경
                reservationService.cancelReservationsByPayment(payment);

                return true;
            }
        } catch (Exception e) {
            log.error("환불 중 오류 발생", e);
        }
        return false;
    }

}
