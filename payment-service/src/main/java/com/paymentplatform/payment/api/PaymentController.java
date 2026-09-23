package com.paymentplatform.payment.api;

import com.paymentplatform.payment.service.PaymentCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

    public PaymentController(PaymentCommandService paymentCommandService) {
        this.paymentCommandService = paymentCommandService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 255) String idempotencyKey) {
        PaymentResponse payment = paymentCommandService.createPayment(request, idempotencyKey);
        return ResponseEntity.created(URI.create("/api/payments/" + payment.id()))
                .status(HttpStatus.CREATED)
                .body(payment);
    }
}
