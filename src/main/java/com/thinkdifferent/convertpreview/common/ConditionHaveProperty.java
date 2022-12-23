package com.thinkdifferent.convertpreview.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Objects;

/**
 * 配置信息都存在时满足条件
 *
 * @author ltian
 * @version 1.0
 * @date 2022/8/25 15:27
 */
public class ConditionHaveProperty implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MultiValueMap<String, Object> allAnnotationAttributes = metadata.getAllAnnotationAttributes(ConditionalHaveProperty.class.getName());
        if (Objects.isNull(allAnnotationAttributes)){
            return false;
        }
        List<Object> value = allAnnotationAttributes.get("value");
        if (value.get(0) instanceof String[]) {
            for (String key : (String[]) value.get(0)) {
                if (StringUtils.isBlank(context.getEnvironment().getProperty(key))) {
                    return false;
                }
            }
        }
        return true;
    }
}
