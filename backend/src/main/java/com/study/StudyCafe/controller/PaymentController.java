package com.study.StudyCafe.controller;

import com.study.StudyCafe.dto.payment.PaymentDetailDto;
import com.study.StudyCafe.dto.payment.PaymentRequestDto;
import com.study.StudyCafe.dto.payment.PaymentResponseDto;
import com.study.StudyCafe.entity.User;
import com.study.StudyCafe.repository.UserRepository;
import com.study.StudyCafe.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    // ✅ 결제 검증 및 예약
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDto> verifyAndProcessPayment(
            @RequestBody PaymentRequestDto dto
    ) {
        boolean result = paymentService.verifyAndReserve(dto);

        PaymentResponseDto response = new PaymentResponseDto();
        response.setMerchantUid(dto.getMerchantUid());
        response.setAmount(dto.getAmount());
        response.setStatus(result ? "PAID" : "FAILED");
        response.setMessage(result ? "결제 및 예약 완료" : "결제 실패 또는 예약 불가");

        return ResponseEntity.ok(response);
    }

//    // ✅ 결제 상세 조회
//    @GetMapping("/{merchantUid}")
//    public ResponseEntity<PaymentDetailDto> getPaymentDetail(@PathVariable String merchantUid) {
//        return ResponseEntity.ok(paymentService.getPaymentDetail(merchantUid));
//    }
//
//    // ✅ 사용자 결제 내역 조회
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<List<PaymentDetailDto>> getUserPayments(@PathVariable Long userId) {
//        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
//    }

    // ✅ 결제 취소
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelPayment(
            @RequestParam String impUid,
            @RequestParam(required = false, defaultValue = "사용자 요청") String reason,
            @AuthenticationPrincipal String email
    ) {
        if (email == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        boolean result = paymentService.cancelPaymentByImpUid(impUid, reason);
        return ResponseEntity.ok(result ? "SUCCESS" : "FAIL");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyPayments(
            @AuthenticationPrincipal String email  // ✅ 변경된 부분
    ) {
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 정보 없음"));

        Long userId = user.getId();
        List<PaymentDetailDto> payments = paymentService.getPaymentsByUser(userId);
        return ResponseEntity.ok(payments);
    }
}
