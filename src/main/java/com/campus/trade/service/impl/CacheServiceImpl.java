package com.campus.trade.service.impl;

import com.campus.trade.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class CacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public CacheServiceImpl(RedisTemplate<String, Object> redisTemplate,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void bumpVersion(String versionKey) {
        try {
            stringRedisTemplate.opsForValue().increment(versionKey);
        } catch (Exception e) {
            logger.warn("缓存版本号自增失败(Redis不可用，忽略): {}", e.getMessage());
        }
    }

    @Override
    public long getVersion(String versionKey) {
        try {
            String v = stringRedisTemplate.opsForValue().get(versionKey);
            return v == null ? 0L : Long.parseLong(v);
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public <T> T readThrough(String cacheKey, String lockKey, long baseTtlSeconds,
                             long nullTtlSeconds, Supplier<T> loader, Class<T> type) {
        // 1. 直接读缓存
        Object cached = getQuietly(cacheKey);
        if (cached != null) {
            if (NULL_MARK.equals(cached)) {
                return null;  // 空值标记：查库也无结果，直接返回 null
            }
            return convert(cached, type);
        }

        // 2. 互斥锁重建缓存（防击穿）
        if (tryLock(lockKey)) {
            try {
                // 双检：锁竞争期间可能已有线程写入
                cached = getQuietly(cacheKey);
                if (cached != null) {
                    return NULL_MARK.equals(cached) ? null : convert(cached, type);
                }
                T value = loader.get();
                if (value == null) {
                    putNull(cacheKey, nullTtlSeconds);  // 空值标记防穿透
                } else {
                    put(cacheKey, value, baseTtlSeconds);
                }
                return value;
            } finally {
                releaseLock(lockKey);
            }
        }

        // 3. 未抢到锁：短暂自旋后重试读缓存
        for (int i = 0; i < 5; i++) {
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            cached = getQuietly(cacheKey);
            if (cached != null) {
                return NULL_MARK.equals(cached) ? null : convert(cached, type);
            }
        }

        // 4. 兜底：直接查库（避免无限等待），写入缓存
        try {
            T value = loader.get();
            if (value == null) {
                putNull(cacheKey, nullTtlSeconds);
            } else {
                put(cacheKey, value, baseTtlSeconds);
            }
            return value;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void put(String key, Object value, long baseTtlSeconds) {
        try {
            // TTL 抖动：base ± 10% 随机，防雪崩
            long jitter = baseTtlSeconds > 0 ? ThreadLocalRandom.current().nextLong(-baseTtlSeconds / 10, baseTtlSeconds / 10 + 1) : 0;
            long ttl = Math.max(1, baseTtlSeconds + jitter);
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("缓存写入失败(Redis不可用，忽略): {}", e.getMessage());
        }
    }

    @Override
    public void evict(String... keys) {
        try {
            redisTemplate.delete(java.util.List.of(keys));
        } catch (Exception e) {
            logger.warn("缓存删除失败(Redis不可用，忽略): {}", e.getMessage());
        }
    }

    private void putNull(String cacheKey, long nullTtlSeconds) {
        try {
            redisTemplate.opsForValue().set(cacheKey, NULL_MARK, nullTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("空值标记写入失败(Redis不可用，忽略): {}", e.getMessage());
        }
    }

    private Object getQuietly(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.warn("缓存读取失败(Redis不可用，降级直查DB): {}", e.getMessage());
            return null;
        }
    }

    private boolean tryLock(String lockKey) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            return false;  // Redis 不可用时放行，直接走查库
        }
    }

    private void releaseLock(String lockKey) {
        try {
            stringRedisTemplate.delete(lockKey);
        } catch (Exception ignored) {}
    }

    private <T> T convert(Object cached, Class<T> type) {
        if (type.isInstance(cached)) {
            return type.cast(cached);
        }
        return objectMapper.convertValue(cached, type);
    }
}
