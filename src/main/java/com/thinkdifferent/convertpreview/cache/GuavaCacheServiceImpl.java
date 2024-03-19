package com.thinkdifferent.convertpreview.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

/**
 * guava 缓存，只支持一种过期时间, 8H过期
 *
 * @author ltian
 * @version 1.0
 * @date 2022/5/26 14:42
 */
@Service
@Conditional(LocalEnabled.class)
public class GuavaCacheServiceImpl implements CacheService {

    Cache<String, Object> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(8)).build();

    /**
     * 存储值
     *
     * @param key           key
     * @param value         value
     * @param expireSeconds 过期时间(秒)
     * @return bln
     */
    @Override
    public boolean set(String key, Object value, long expireSeconds) {
        cache.put(key, value);
        return true;
    }

    /**
     * @param key 存入的值
     * @return 缓存中的值， 不存在返回空
     */
    @Override
    public Object get(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * @param key 需要移除的key
     */
    @Override
    public void remove(String key) {
        cache.invalidate(key);
    }

    /**
     * 是否存在对应的key
     *
     * @param key key
     * @return 存在：true；不存在：false
     */
    @Override
    public boolean exists(String key) {
        return Objects.nonNull(cache.getIfPresent(key));
    }
}
