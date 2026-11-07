package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByIdAndOrderUserId(Long id, Long userId);

    List<Payment> findByOrderId(Long orderId);
}
