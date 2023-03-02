package com.thinkdifferent.convertpreview.cache;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/5/26 14:21
 */
@Component
public class CacheManager {
    /**
     * 默认过期时间，s, 本地缓存无法生效
     */
    private static final long DEFAULT_EXPIRE_SECONDS = 60 * 60L;
    @Resource
    private CacheService cacheService;

    public boolean set(String key, Object value) {
        return set(key, value, DEFAULT_EXPIRE_SECONDS);
    }

    public boolean set(String key, Object value, long expireSecond) {
        return cacheService.set(key, value, expireSecond);
    }

    public Object get(String key) {
        return cacheService.get(key);
    }

    public String getString(String key) {
        Object value = cacheService.get(key);
        return Objects.isNull(value) ? null : value.toString();
    }

    public boolean exists(String key) {
        return cacheService.exists(key);
    }
}