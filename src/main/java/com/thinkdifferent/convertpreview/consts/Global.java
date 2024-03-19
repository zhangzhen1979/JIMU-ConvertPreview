package com.thinkdifferent.convertpreview.consts;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.util.concurrent.TimeUnit;

public class Global {
    /**
     * 用于存储Ticket的Map
     */
    public static final ExpiringMap<String,String> MAP_TICKET = ExpiringMap.builder()
            .maxSize(100)
            .expiration(1, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .variableExpiration()
            .build();

    /**
     * 转换类型标识字段
     */
    public static final String CONVERT_TYPE = "convert_type";
}
