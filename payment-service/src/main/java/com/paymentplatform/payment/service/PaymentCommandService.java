package com.paymentplatform.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.payment.api.CreatePaymentRequest;
import com.paymentplatform.payment.api.PaymentCreationResponse;
import com.paymentplatform.payment.api.PaymentResponse;
import com.paymentplatform.payment.domain.Account;
import com.paymentplatform.payment.domain.IdempotencyRecord;
import com.paymentplatform.payment.domain.Payment;
import com.paymentplatform.payment.exception.IdempotencyConflictException;
import com.paymentplatform.payment.exception.InvalidPaymentRequestException;
import com.paymentplatform.payment.exception.ResourceNotFoundException;
import com.paymentplatform.payment.repository.AccountRepository;
import com.paymentplatform.payment.repository.IdempotencyRecordRepository;
import com.paymentplatform.payment.repository.PaymentRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCommandService {
    private final AccountRepository accountRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public PaymentCommandService(
            AccountRepository accountRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            PaymentRepository paymentRepository,
            ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentCreationResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        String requestHash = requestHash(request);
        var existing = idempotencyRecordRepository.findByScopeAndIdempotencyKey("payments.create", idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.hasRequestHash(requestHash)) {
                throw new IdempotencyConflictException();
            }
            if (record.getPayment() == null) {
                throw new InvalidPaymentRequestException("A request with this Idempotency-Key is still being processed");
            }
            return new PaymentCreationResponse(PaymentResponse.from(record.getPayment()), false);
        }

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
        IdempotencyRecord record = IdempotencyRecord.inProgress("payments.create", idempotencyKey, requestHash);
        Payment savedPayment = paymentRepository.save(payment);
        PaymentResponse response = PaymentResponse.from(savedPayment);
        record.complete(savedPayment, serialize(response));
        idempotencyRecordRepository.save(record);
        return new PaymentCreationResponse(response, true);
    }

    private String requestHash(CreatePaymentRequest request) {
        String canonicalRequest = String.join(
                "|",
                request.payerAccountId().toString(),
                request.payeeAccountId().toString(),
                request.amount().stripTrailingZeros().toPlainString(),
                request.currency(),
                request.merchantReference());
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String serialize(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize idempotent response", exception);
        }
    }
}
