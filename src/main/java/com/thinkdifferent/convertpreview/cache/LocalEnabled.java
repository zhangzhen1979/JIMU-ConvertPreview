package com.thinkdifferent.convertpreview.cache;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Objects;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/5/28 14:30
 */
@Log4j2
public class LocalEnabled implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            Objects.requireNonNull(context.getBeanFactory()).getBean(CacheService.class);
            return false;
        } catch (Exception | Error e) {
            log.info("使用默认缓存");
            return true;
        }
    }
}
