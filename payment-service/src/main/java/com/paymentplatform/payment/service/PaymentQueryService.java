package com.paymentplatform.payment.service;

import com.paymentplatform.payment.api.PaymentResponse;
import com.paymentplatform.payment.exception.ResourceNotFoundException;
import com.paymentplatform.payment.repository.PaymentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentQueryService {
    private final PaymentRepository paymentRepository;

    public PaymentQueryService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        return paymentRepository
                .findById(paymentId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("payment was not found"));
    }
}
