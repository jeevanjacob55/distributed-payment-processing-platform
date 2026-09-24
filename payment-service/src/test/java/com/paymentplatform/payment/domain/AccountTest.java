package com.paymentplatform.payment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AccountTest {
    @Test
    void refusesADebitThatWouldOverdrawTheAvailableBalance() {
        Account account = accountWithBalance("10.00");

        assertThrows(IllegalStateException.class, () -> account.debit(new BigDecimal("10.01")));
        assertEquals(new BigDecimal("10.00"), account.getAvailableBalance());
    }

    @Test
    void debitsAndCreditsTheAvailableBalance() {
        Account account = accountWithBalance("10.00");

        account.debit(new BigDecimal("4.25"));
        account.credit(new BigDecimal("1.25"));

        assertEquals(new BigDecimal("7.00"), account.getAvailableBalance());
    }

    private Account accountWithBalance(String balance) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "availableBalance", new BigDecimal(balance));
        return account;
    }
}
