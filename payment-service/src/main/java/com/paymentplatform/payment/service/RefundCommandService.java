package com.paymentplatform.payment.service;

import com.paymentplatform.payment.api.CreateRefundRequest;
import com.paymentplatform.payment.api.RefundResponse;
import com.paymentplatform.payment.domain.Account;
import com.paymentplatform.payment.domain.AccountStatus;
import com.paymentplatform.payment.domain.Payment;
import com.paymentplatform.payment.domain.PaymentStatus;
import com.paymentplatform.payment.domain.Refund;
import com.paymentplatform.payment.exception.DuplicateReferenceException;
import com.paymentplatform.payment.exception.PaymentRejectedException;
import com.paymentplatform.payment.exception.ResourceNotFoundException;
import com.paymentplatform.payment.repository.AccountRepository;
import com.paymentplatform.payment.repository.PaymentRepository;
import com.paymentplatform.payment.repository.RefundRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundCommandService {
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public RefundCommandService(
            AccountRepository accountRepository, PaymentRepository paymentRepository, RefundRepository refundRepository) {
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional
    public RefundResponse createRefund(UUID paymentId, CreateRefundRequest request) {
        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("payment was not found"));
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentRejectedException("only completed payments can be refunded");
        }
        if (refundRepository.existsByReference(request.reference())) {
            throw new DuplicateReferenceException("refund reference has already been used");
        }
        BigDecimal refundedAmount = refundRepository.totalCompletedAmountByPaymentId(paymentId);
        if (refundedAmount.add(request.amount()).compareTo(payment.getAmount()) > 0) {
            throw new PaymentRejectedException("refund total cannot exceed the original payment amount");
        }

        Account[] accounts = loadAccountsForUpdate(payment);
        Account payer = accounts[0];
        Account payee = accounts[1];
        if (payer.getStatus() != AccountStatus.ACTIVE || payee.getStatus() != AccountStatus.ACTIVE) {
            throw new PaymentRejectedException("payer and payee accounts must be active to process a refund");
        }
        if (payee.getAvailableBalance().compareTo(request.amount()) < 0) {
            throw new PaymentRejectedException("payee account has insufficient available balance for the refund");
        }
        payee.debit(request.amount());
        payer.credit(request.amount());

        Refund refund = refundRepository.save(Refund.create(payment, request.amount(), request.reference()));
        if (refundedAmount.add(request.amount()).compareTo(payment.getAmount()) == 0) {
            payment.transitionTo(PaymentStatus.REVERSED);
        }
        return RefundResponse.from(refund);
    }

    private Account[] loadAccountsForUpdate(Payment payment) {
        UUID payerId = payment.getPayerAccount().getId();
        UUID payeeId = payment.getPayeeAccount().getId();
        UUID firstId = payerId.compareTo(payeeId) < 0 ? payerId : payeeId;
        UUID secondId = firstId.equals(payerId) ? payeeId : payerId;
        Account first = accountRepository
                .findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("payer account was not found"));
        Account second = accountRepository
                .findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("payee account was not found"));
        return payerId.equals(firstId) ? new Account[] {first, second} : new Account[] {second, first};
    }
}
