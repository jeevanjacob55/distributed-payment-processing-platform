package com.paymentplatform.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.payment.api.CreatePaymentRequest;
import com.paymentplatform.payment.api.PaymentCreationResponse;
import com.paymentplatform.payment.api.PaymentResponse;
import com.paymentplatform.payment.domain.Account;
import com.paymentplatform.payment.domain.AccountStatus;
import com.paymentplatform.payment.domain.IdempotencyRecord;
import com.paymentplatform.payment.domain.Payment;
import com.paymentplatform.payment.domain.PaymentStatus;
import com.paymentplatform.payment.event.PaymentEventPublisher;
import com.paymentplatform.payment.exception.PaymentProcessingException;
import com.paymentplatform.payment.exception.DuplicateReferenceException;
import com.paymentplatform.payment.exception.IdempotencyConflictException;
import com.paymentplatform.payment.exception.InvalidPaymentRequestException;
import com.paymentplatform.payment.exception.PaymentRejectedException;
import com.paymentplatform.payment.exception.ResourceNotFoundException;
import com.paymentplatform.payment.repository.AccountRepository;
import com.paymentplatform.payment.repository.IdempotencyRecordRepository;
import com.paymentplatform.payment.repository.PaymentRepository;
import com.paymentplatform.payment.lock.IdempotencyLock;
import com.paymentplatform.payment.lock.IdempotencyLockService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PaymentCommandService {
    private final AccountRepository accountRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final BigDecimal maxPaymentAmount;
    private final PaymentEventPublisher paymentEventPublisher;
    private final IdempotencyLockService idempotencyLockService;

    public PaymentCommandService(
            AccountRepository accountRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            PaymentRepository paymentRepository,
            ObjectMapper objectMapper,
            @Value("${payment.max-amount:1000000.0000}") BigDecimal maxPaymentAmount,
            PaymentEventPublisher paymentEventPublisher,
            IdempotencyLockService idempotencyLockService) {
        this.accountRepository = accountRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.maxPaymentAmount = maxPaymentAmount;
        this.paymentEventPublisher = paymentEventPublisher;
        this.idempotencyLockService = idempotencyLockService;
    }

    @Transactional
    public PaymentCreationResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        String requestHash = requestHash(request);
        Optional<PaymentCreationResponse> replay = findIdempotentReplay(idempotencyKey, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }

        IdempotencyLock lock = idempotencyLockService
                .tryAcquire(idempotencyKey)
                .orElseThrow(PaymentProcessingException::new);
        releaseAfterTransactionCompletion(lock);

        if (request.payerAccountId().equals(request.payeeAccountId())) {
            throw new InvalidPaymentRequestException("payer and payee accounts must be different");
        }

        Account[] accounts = loadAccountsForUpdate(request.payerAccountId(), request.payeeAccountId());
        Account payer = accounts[0];
        Account payee = accounts[1];

        // The account locks serialize same-payer requests. Rechecking the key after acquiring
        // them closes the race where identical requests both observed no idempotency record.
        replay = findIdempotentReplay(idempotencyKey, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }
        validatePayment(request, payer, payee);

        Payment payment = Payment.create(
                payer,
                payee,
                request.amount(),
                request.currency(),
                request.merchantReference(),
                idempotencyKey);
        IdempotencyRecord record = IdempotencyRecord.inProgress("payments.create", idempotencyKey, requestHash);
        Payment savedPayment = paymentRepository.save(payment);
        payer.debit(request.amount());
        payee.credit(request.amount());
        savedPayment.transitionTo(PaymentStatus.VALIDATED);
        savedPayment.transitionTo(PaymentStatus.AUTHORIZED);
        savedPayment.transitionTo(PaymentStatus.COMPLETED);
        paymentEventPublisher.publishCompleted(savedPayment);
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

    private Optional<PaymentCreationResponse> findIdempotentReplay(String idempotencyKey, String requestHash) {
        return idempotencyRecordRepository
                .findByScopeAndIdempotencyKey("payments.create", idempotencyKey)
                .map(record -> replay(record, requestHash));
    }

    private PaymentCreationResponse replay(IdempotencyRecord record, String requestHash) {
        if (!record.hasRequestHash(requestHash)) {
            throw new IdempotencyConflictException();
        }
        if (record.getPayment() == null) {
            throw new InvalidPaymentRequestException("A request with this Idempotency-Key is still being processed");
        }
        return new PaymentCreationResponse(PaymentResponse.from(record.getPayment()), false);
    }

    private String serialize(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize idempotent response", exception);
        }
    }

    private Account[] loadAccountsForUpdate(UUID payerAccountId, UUID payeeAccountId) {
        UUID firstId = payerAccountId.compareTo(payeeAccountId) < 0 ? payerAccountId : payeeAccountId;
        UUID secondId = firstId.equals(payerAccountId) ? payeeAccountId : payerAccountId;
        Account first = accountRepository
                .findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("account was not found"));
        Account second = accountRepository
                .findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("account was not found"));
        return payerAccountId.equals(firstId) ? new Account[] {first, second} : new Account[] {second, first};
    }

    private void validatePayment(CreatePaymentRequest request, Account payer, Account payee) {
        if (paymentRepository
                .findByPayerAccount_IdAndMerchantReference(payer.getId(), request.merchantReference())
                .isPresent()) {
            throw new DuplicateReferenceException("merchant reference has already been used for this payer account");
        }
        if (payer.getStatus() != AccountStatus.ACTIVE || payee.getStatus() != AccountStatus.ACTIVE) {
            throw new PaymentRejectedException("payer and payee accounts must be active");
        }
        if (!request.currency().equals(payer.getCurrency()) || !request.currency().equals(payee.getCurrency())) {
            throw new PaymentRejectedException("payment currency must match both account currencies");
        }
        if (payer.getAvailableBalance().compareTo(request.amount()) < 0) {
            throw new PaymentRejectedException("payer account has insufficient available balance");
        }
        if (request.amount().compareTo(maxPaymentAmount) > 0) {
            throw new PaymentRejectedException("payment amount exceeds the configured limit");
        }
    }

    private void releaseAfterTransactionCompletion(IdempotencyLock lock) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.close();
            }
        });
    }
}
