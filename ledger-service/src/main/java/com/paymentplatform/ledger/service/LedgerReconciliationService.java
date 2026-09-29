package com.paymentplatform.ledger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(LedgerReconciliationService.class);
    private final JdbcTemplate jdbcTemplate;

    public LedgerReconciliationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${ledger.reconciliation.cron:0 */5 * * * *}")
    @Transactional
    public void reconcile() {
        int checked = jdbcTemplate.update("""
                WITH totals AS (
                    SELECT account_id,
                           currency,
                           SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END) AS balance
                      FROM ledger.ledger_entries
                     WHERE status = 'POSTED'
                     GROUP BY account_id, currency
                ), comparisons AS (
                    SELECT COALESCE(p.account_id, t.account_id) AS account_id,
                           COALESCE(p.currency, t.currency) AS currency,
                           COALESCE(p.available_balance, 0) AS available_balance,
                           COALESCE(p.posted_balance, 0) AS posted_balance,
                           COALESCE(t.balance, 0) AS calculated_balance
                      FROM ledger.account_balance_projections p
                      FULL OUTER JOIN totals t
                        ON t.account_id = p.account_id AND t.currency = p.currency
                )
                INSERT INTO ledger.reconciliation_issues
                    (account_id, currency, available_balance, posted_balance, calculated_balance, discrepancy, balanced, checked_at)
                SELECT account_id,
                       currency,
                       available_balance,
                       posted_balance,
                       calculated_balance,
                       calculated_balance - posted_balance,
                       available_balance = calculated_balance AND posted_balance = calculated_balance,
                       CURRENT_TIMESTAMP
                  FROM comparisons
                ON CONFLICT (account_id, currency) DO UPDATE
                   SET available_balance = EXCLUDED.available_balance,
                       posted_balance = EXCLUDED.posted_balance,
                       calculated_balance = EXCLUDED.calculated_balance,
                       discrepancy = EXCLUDED.discrepancy,
                       balanced = EXCLUDED.balanced,
                       checked_at = EXCLUDED.checked_at
                """);
        Integer imbalanced = jdbcTemplate.queryForObject(
                "select count(*) from ledger.reconciliation_issues where not balanced", Integer.class);
        if (imbalanced != null && imbalanced > 0) {
            log.error("Ledger reconciliation found {} imbalanced account/currency projections among {} checked", imbalanced, checked);
        } else {
            log.info("Ledger reconciliation checked {} account/currency projections", checked);
        }
    }
}
