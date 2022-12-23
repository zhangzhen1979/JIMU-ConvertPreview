package com.thinkdifferent.convertpreview.common;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置项都存在的情况下通过
 */
@Conditional(ConditionHaveProperty.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalHaveProperty {
    String[] value() default {};
}
