package com.paymentplatform.payment.lock;

import com.paymentplatform.payment.exception.DistributedLockUnavailableException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyLockService {
    private static final DefaultRedisScript<Long> RELEASE_IF_OWNER = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration lockTtl;

    public IdempotencyLockService(
            StringRedisTemplate redisTemplate,
            @Value("${payment.idempotency-lock.ttl:PT30S}") Duration lockTtl) {
        this.redisTemplate = redisTemplate;
        this.lockTtl = lockTtl;
    }

    public Optional<IdempotencyLock> tryAcquire(String idempotencyKey) {
        String lockKey = "payment:idempotency-lock:" + sha256(idempotencyKey);
        String ownerToken = UUID.randomUUID().toString();
        try {
            if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, ownerToken, lockTtl))) {
                return Optional.empty();
            }
            return Optional.of(() -> release(lockKey, ownerToken));
        } catch (DataAccessException exception) {
            throw new DistributedLockUnavailableException(exception);
        }
    }

    private void release(String lockKey, String ownerToken) {
        try {
            redisTemplate.execute(RELEASE_IF_OWNER, java.util.List.of(lockKey), ownerToken);
        } catch (DataAccessException ignored) {
            // The TTL bounds a failed unlock. Financial correctness remains protected by PostgreSQL.
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
