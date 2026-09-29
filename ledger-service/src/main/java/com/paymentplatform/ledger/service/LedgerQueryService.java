package com.paymentplatform.ledger.service;

import com.paymentplatform.ledger.api.LedgerBalanceResponse;
import com.paymentplatform.ledger.api.LedgerEntryResponse;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerQueryService {
    private static final RowMapper<LedgerEntryResponse> ENTRY_MAPPER = LedgerQueryService::mapEntry;
    private final JdbcTemplate jdbcTemplate;

    public LedgerQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public LedgerBalanceResponse getBalance(UUID accountId, String currency) {
        List<LedgerBalanceResponse> rows = jdbcTemplate.query(
                "select account_id, currency, available_balance, posted_balance, updated_at from ledger.account_balance_projections where account_id = ? and currency = ?",
                (rs, rowNum) -> new LedgerBalanceResponse(
                        rs.getObject("account_id", UUID.class),
                        rs.getString("currency").trim(),
                        rs.getBigDecimal("available_balance"),
                        rs.getBigDecimal("posted_balance"),
                        rs.getTimestamp("updated_at").toInstant()),
                accountId,
                currency);
        if (!rows.isEmpty()) {
            return rows.getFirst();
        }
        return new LedgerBalanceResponse(accountId, currency, BigDecimal.ZERO, BigDecimal.ZERO, null);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getTransactions(
            UUID accountId, String currency, String status, int page, int size) {
        StringBuilder where = new StringBuilder(" where e.account_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(accountId);
        if (currency != null) {
            where.append(" and e.currency = ?");
            parameters.add(currency);
        }
        if (status != null) {
            where.append(" and e.status = ?");
            parameters.add(status);
        }

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from ledger.ledger_entries e" + where, Long.class, parameters.toArray());
        List<Object> pageParameters = new ArrayList<>(parameters);
        pageParameters.add(size);
        pageParameters.add((long) page * size);
        List<LedgerEntryResponse> content = jdbcTemplate.query(
                "select e.id, e.transaction_id, t.reference_type, t.reference_id, t.description, e.account_id, e.direction, e.amount, e.currency, e.status, e.created_at "
                        + "from ledger.ledger_entries e join ledger.ledger_transactions t on t.id = e.transaction_id"
                        + where + " order by e.created_at desc, e.id desc limit ? offset ?",
                ENTRY_MAPPER,
                pageParameters.toArray());
        return new PageImpl<>(content, PageRequest.of(page, size), total == null ? 0 : total);
    }

    private static LedgerEntryResponse mapEntry(ResultSet rs, int rowNum) throws SQLException {
        return new LedgerEntryResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("transaction_id", UUID.class),
                rs.getString("reference_type"),
                rs.getObject("reference_id", UUID.class),
                rs.getString("description"),
                rs.getObject("account_id", UUID.class),
                rs.getString("direction"),
                rs.getBigDecimal("amount"),
                rs.getString("currency").trim(),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant());
    }
}
