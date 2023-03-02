package com.thinkdifferent.convertpreview.consts;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 公共配置信息
 * @author ltian
 * @version 1.0
 * @date 2022/12/9 10:42
 */
@Component
public class ConfigConstants {
    /**
     * 不带末尾 /
     */
    public static String baseUrl;

    @Value("${base.url:}")
    public void setBaseUrl(String baseUrl) {
        if (StringUtils.endsWith(baseUrl, "/")){
            ConfigConstants.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }else{
            ConfigConstants.baseUrl = baseUrl;
        }
    }
}
