package com.paymentplatform.ledger.api;

import com.paymentplatform.ledger.service.LedgerQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/accounts/{accountId}")
public class LedgerController {
    private final LedgerQueryService queryService;

    public LedgerController(LedgerQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/balance")
    public LedgerBalanceResponse getBalance(
            @PathVariable UUID accountId,
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String currency) {
        return queryService.getBalance(accountId, currency);
    }

    @GetMapping("/transactions")
    public Page<LedgerEntryResponse> getTransactions(
            @PathVariable UUID accountId,
            @RequestParam(required = false) @Pattern(regexp = "[A-Z]{3}") String currency,
            @RequestParam(required = false) @Pattern(regexp = "PENDING|POSTED|REVERSED") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return queryService.getTransactions(accountId, currency, status, page, size);
    }
}
