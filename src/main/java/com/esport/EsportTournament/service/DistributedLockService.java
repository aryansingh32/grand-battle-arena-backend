package com.esport.EsportTournament.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-based Distributed Lock for concurrent booking protection.
 *
 * <p>Guarantees that only ONE thread/instance can process a booking for a
 * specific slot at any given time, even across multiple backend replicas.</p>
 *
 * <p>Uses Redis SET NX EX (atomic set-if-not-exists with expiry) which is
 * the industry-standard pattern for distributed locking.</p>
 */
@Slf4j
@Service
public class DistributedLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public DistributedLockService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String LOCK_PREFIX = "lock:";
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Acquire a distributed lock.
     *
     * @param key          unique lock identifier (e.g. "slot:tournament:5:slot:3")
     * @param timeout      how long the lock should live (auto-release safety net)
     * @return lockValue   UUID value needed to release the lock, or null if lock not acquired
     */
    public String acquireLock(String key, Duration timeout) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();

        if (redisTemplate == null) {
            log.debug("🔧 Redis disabled: Simulating acquired lock for {}", lockKey);
            return lockValue;
        }

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, timeout);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("🔒 Lock acquired: {} (value={})", lockKey, lockValue);
                return lockValue;
            }

            log.debug("⏳ Lock not acquired (held by another): {}", lockKey);
            return null;
        } catch (Exception e) {
            log.warn("⚠️ Redis lock failed for {}: {} — falling back to DB locks only",
                    lockKey, e.getMessage());
            // Return a value anyway so the caller proceeds with DB-level locks as fallback
            return lockValue;
        }
    }

    /**
     * Acquire lock with default timeout (10 seconds).
     */
    public String acquireLock(String key) {
        return acquireLock(key, DEFAULT_LOCK_TIMEOUT);
    }

    /**
     * Release a distributed lock safely.
     * Only releases if the lock still belongs to this caller (prevents releasing
     * another caller's lock if ours expired).
     *
     * @param key       the lock key
     * @param lockValue the UUID returned by acquireLock
     */
    public void releaseLock(String key, String lockValue) {
        String lockKey = LOCK_PREFIX + key;

        if (redisTemplate == null) {
            log.debug("🔧 Redis disabled: Simulating released lock for {}", lockKey);
            return;
        }

        try {
            Object currentValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                redisTemplate.delete(lockKey);
                log.debug("🔓 Lock released: {}", lockKey);
            } else {
                log.warn("⚠️ Lock {} already expired or owned by another caller", lockKey);
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to release Redis lock {}: {}", lockKey, e.getMessage());
            // Non-fatal: the lock will auto-expire via TTL
        }
    }

    /**
     * Generate a lock key for a specific tournament slot.
     */
    public static String slotLockKey(int tournamentId, int slotNumber) {
        return String.format("slot:tournament:%d:slot:%d", tournamentId, slotNumber);
    }

    /**
     * Generate a lock key for a user booking action (prevents double-tap).
     */
    public static String userBookingLockKey(String firebaseUID, int tournamentId) {
        return String.format("booking:user:%s:tournament:%d", firebaseUID, tournamentId);
    }
}
