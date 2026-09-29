package com.paymentplatform.fraud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.fraud.api.FraudRuleResponse;
import com.paymentplatform.fraud.api.UpsertFraudRuleRequest;
import com.paymentplatform.fraud.domain.FraudRuleAction;
import com.paymentplatform.fraud.domain.FraudRuleType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FraudRuleService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public FraudRuleService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<FraudRuleResponse> list() {
        return jdbcTemplate.query(
                "select id, code, rule_type, action, enabled, parameters, created_at, updated_at from fraud.fraud_rules order by code",
                this::mapRule);
    }

    @Transactional
    public FraudRuleResponse upsert(String pathCode, UpsertFraudRuleRequest request) {
        if (!pathCode.equals(request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rule code in path and body must match");
        }
        validateParameters(request.type(), request.parameters());
        UUID id = jdbcTemplate.query(
                        "insert into fraud.fraud_rules (id, code, rule_type, action, enabled, parameters) values (?, ?, ?, ?, ?, ?::jsonb) "
                                + "on conflict (code) do update set rule_type = excluded.rule_type, action = excluded.action, enabled = excluded.enabled, parameters = excluded.parameters, updated_at = CURRENT_TIMESTAMP "
                                + "returning id",
                        (rs, rowNum) -> rs.getObject("id", UUID.class),
                        UUID.randomUUID(),
                        request.code(),
                        request.type().name(),
                        request.action().name(),
                        request.enabled(),
                        writeJson(request.parameters()))
                .getFirst();
        return get(id);
    }

    @Transactional
    public void delete(String code) {
        int deleted = jdbcTemplate.update("delete from fraud.fraud_rules where code = ?", code);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "fraud rule was not found");
        }
    }

    @Transactional(readOnly = true)
    public List<FraudRuleResponse> enabledRules() {
        return jdbcTemplate.query(
                "select id, code, rule_type, action, enabled, parameters, created_at, updated_at from fraud.fraud_rules where enabled = true order by code",
                this::mapRule);
    }

    private FraudRuleResponse get(UUID id) {
        List<FraudRuleResponse> rows = jdbcTemplate.query(
                "select id, code, rule_type, action, enabled, parameters, created_at, updated_at from fraud.fraud_rules where id = ?",
                this::mapRule,
                id);
        return rows.getFirst();
    }

    private void validateParameters(FraudRuleType type, JsonNode parameters) {
        if (parameters == null || !parameters.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rule parameters must be a JSON object");
        }
        switch (type) {
            case MAX_AMOUNT -> requirePositiveDecimal(parameters, "threshold");
            case VELOCITY -> {
                requirePositiveInteger(parameters, "maxTransactions");
                requirePositiveInteger(parameters, "windowMinutes");
            }
            case REPEATED_REFERENCE -> {
                requirePositiveInteger(parameters, "maxAttempts");
                requirePositiveInteger(parameters, "windowMinutes");
            }
            case MAX_VOLUME -> {
                requirePositiveDecimal(parameters, "maxVolume");
                requirePositiveInteger(parameters, "windowMinutes");
            }
            case BLOCKED_ACCOUNT -> {
                JsonNode accountIds = parameters.get("accountIds");
                if (accountIds == null || !accountIds.isArray() || accountIds.isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "BLOCKED_ACCOUNT requires a non-empty accountIds array");
                }
                accountIds.forEach(node -> {
                    try {
                        UUID.fromString(node.asText());
                    } catch (IllegalArgumentException exception) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "accountIds must contain UUID values");
                    }
                });
            }
        }
    }

    private void requirePositiveDecimal(JsonNode parameters, String name) {
        JsonNode value = parameters.get(name);
        if (value == null || !value.isNumber() || value.decimalValue().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " must be a positive number");
        }
    }

    private void requirePositiveInteger(JsonNode parameters, String name) {
        JsonNode value = parameters.get(name);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " must be a positive integer");
        }
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize fraud rule parameters", exception);
        }
    }

    private FraudRuleResponse mapRule(ResultSet rs, int rowNum) throws SQLException {
        try {
            return new FraudRuleResponse(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    FraudRuleType.valueOf(rs.getString("rule_type")),
                    FraudRuleAction.valueOf(rs.getString("action")),
                    rs.getBoolean("enabled"),
                    objectMapper.readTree(rs.getString("parameters")),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant());
        } catch (JsonProcessingException exception) {
            throw new SQLException("stored fraud rule parameters are invalid JSON", exception);
        }
    }
}
