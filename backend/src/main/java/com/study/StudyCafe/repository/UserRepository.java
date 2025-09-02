package com.study.StudyCafe.repository;

import com.study.StudyCafe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}