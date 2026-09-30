package com.paymentplatform.ledger.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class LedgerPostingServiceTest {
    private JdbcTemplate jdbcTemplate;
    private LedgerPostingService service;
    private UUID payer;
    private UUID payee;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new LedgerPostingService(jdbcTemplate);
        payer = UUID.randomUUID();
        payee = UUID.randomUUID();
    }

    @Test
    void rejectsInvalidAmountsBeforeWritingEntries() {
        PaymentLifecycleEvent event = event(BigDecimal.ZERO, "USD", payer, payee);

        assertThrows(IllegalArgumentException.class, () -> service.post(event));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rejectsInvalidCurrencyAndSameAccountTransfers() {
        assertThrows(IllegalArgumentException.class, () -> service.post(event(
                new BigDecimal("5.00"), "usd", payer, payee)));
        assertThrows(IllegalArgumentException.class, () -> service.post(event(
                new BigDecimal("5.00"), "USD", payer, payer)));

        verifyNoInteractions(jdbcTemplate);
    }

    private PaymentLifecycleEvent event(BigDecimal amount, String currency, UUID payerId, UUID payeeId) {
        return new PaymentLifecycleEvent(
                UUID.randomUUID(), 1, "payment.completed.v1", UUID.randomUUID(), payerId, payeeId,
                amount, currency, "order-1", "COMPLETED", Instant.now());
    }
}
