package com.paymentplatform.payment.service;

import com.paymentplatform.payment.api.CreatePaymentRequest;
import com.paymentplatform.payment.api.PaymentResponse;
import com.paymentplatform.payment.domain.Account;
import com.paymentplatform.payment.domain.Payment;
import com.paymentplatform.payment.exception.InvalidPaymentRequestException;
import com.paymentplatform.payment.exception.ResourceNotFoundException;
import com.paymentplatform.payment.repository.AccountRepository;
import com.paymentplatform.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCommandService {
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;

    public PaymentCommandService(AccountRepository accountRepository, PaymentRepository paymentRepository) {
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        if (request.payerAccountId().equals(request.payeeAccountId())) {
            throw new InvalidPaymentRequestException("payer and payee accounts must be different");
        }

        Account payer = accountRepository.findById(request.payerAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("payer account was not found"));
        Account payee = accountRepository.findById(request.payeeAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("payee account was not found"));

        Payment payment = Payment.create(
                payer,
                payee,
                request.amount(),
                request.currency(),
                request.merchantReference(),
                idempotencyKey);
        return PaymentResponse.from(paymentRepository.save(payment));
    }
}
