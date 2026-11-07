package com.example.ecommerce.service;

import com.example.ecommerce.dto.PaymentRequest;
import com.example.ecommerce.dto.PaymentResponse;
import com.example.ecommerce.entity.AppUser;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Payment;
import com.example.ecommerce.entity.PurchaseOrder;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(UserService userService, OrderRepository orderRepository,
                          PaymentRepository paymentRepository) {
        this.userService = userService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse initiate(String email, Long orderId, PaymentRequest request) {
        AppUser user = userService.requireByEmail(email);
        PurchaseOrder order = orderRepository.findLockedByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " was not found"));
        var existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (!payment.getOrder().getId().equals(orderId) || !payment.getOrder().getUser().getId().equals(user.getId())) {
                throw new ConflictException("Idempotency key has already been used for another payment");
            }
            return PaymentResponse.from(payment);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Payment can only be initiated for an order awaiting payment");
        }
        Payment payment = paymentRepository.save(new Payment(
                order, request.method(), order.getTotal(), request.idempotencyKey()));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(String email, Long paymentId) {
        AppUser user = userService.requireByEmail(email);
        return paymentRepository.findByIdAndOrderUserId(paymentId, user.getId())
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Payment " + paymentId + " was not found"));
    }
}
