package com.thinkdifferent.convertpreview.cache;

/**
 * 缓存服务
 * @author ltian
 * @version 1.0
 * @date 2022/5/26 14:22
 */
public interface CacheService {
    /**
     * 存储值
     * @param key   key
     * @param value value
     * @param expireSeconds 过期时间(秒)
     * @return  bln
     */
    boolean set(String key, Object value, long expireSeconds);

    /**
     * @param key 存入的值
     * @return  缓存中的值， 不存在返回空
     */
    Object get(String key);

    /**
     * @param key 需要移除的key
     */
    void remove(String key);

    /**
     * 是否存在对应的key
     * @param key   key
     * @return  存在：true；不存在：false
     */
    boolean exists(String key);
}
