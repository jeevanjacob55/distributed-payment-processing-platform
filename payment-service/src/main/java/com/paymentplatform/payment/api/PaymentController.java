package com.paymentplatform.payment.api;

import com.paymentplatform.payment.service.PaymentCommandService;
import com.paymentplatform.payment.service.PaymentQueryService;
import com.paymentplatform.payment.service.RefundCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentCommandService paymentCommandService;
    private final PaymentQueryService paymentQueryService;
    private final RefundCommandService refundCommandService;

    public PaymentController(
            PaymentCommandService paymentCommandService,
            PaymentQueryService paymentQueryService,
            RefundCommandService refundCommandService) {
        this.paymentCommandService = paymentCommandService;
        this.paymentQueryService = paymentQueryService;
        this.refundCommandService = refundCommandService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 255) String idempotencyKey) {
        PaymentCreationResponse result = paymentCommandService.createPayment(request, idempotencyKey);
        if (!result.created()) {
            return ResponseEntity.ok(result.payment());
        }
        return ResponseEntity.created(URI.create("/api/payments/" + result.payment().id())).body(result.payment());
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        return paymentQueryService.getPayment(paymentId);
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable UUID paymentId, @Valid @RequestBody CreateRefundRequest request) {
        RefundResponse refund = refundCommandService.createRefund(paymentId, request);
        return ResponseEntity.created(URI.create("/api/payments/" + paymentId + "/refunds/" + refund.id())).body(refund);
    }
}
