package com.study.StudyCafe.repository;

import com.study.StudyCafe.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByImpUid(String impUid);
    Optional<Payment> findByMerchantUid(String merchantUid); // ✅ 상세 조회용

    List<Payment> findByUserId(Long userId); // ✅ 사용자별 결제 목록용

}