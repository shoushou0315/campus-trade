package com.campus.trade.service;

import java.util.function.Supplier;

/**
 * 缓存通用能力：防穿透(空值标记) / 防击穿(互斥锁) / 防雪崩(TTL抖动) / 版本号失效。
 * Redis 故障时自动降级为直查数据库，不影响主流程。
 */
public interface CacheService {

    String NULL_MARK = "\u0000NULL\u0000";

    /** 版本号自增（商品变更时调用，使旧版本搜索缓存整体失效） */
    void bumpVersion(String versionKey);

    /** 读版本号 */
    long getVersion(String versionKey);

    /**
     * 缓存读取（含防穿透+防击穿）：
     * loader 负责查库并返回数据；返回 null 时缓存空值标记（防穿透）。
     * 缓存未命中时互斥锁只让一个线程重建，其余短暂自旋后读缓存。
     */
    <T> T readThrough(String cacheKey, String lockKey, long baseTtlSeconds,
                      long nullTtlSeconds, Supplier<T> loader, Class<T> type);

    /** 写缓存（TTL 加随机抖动防雪崩） */
    void put(String key, Object value, long baseTtlSeconds);

    /** 删除缓存 */
    void evict(String... keys);
}
