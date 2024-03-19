package com.thinkdifferent.convertpreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigEngineOnlyOffice {

    /**
     * 转换引擎：OnlyOffice服务。是否开启。默认：否
     */
    public static Boolean onlyOfficeEnabled;
    @Value("${convert.engine.onlyOffice.enabled:false}")
    public void setOnlyOfficeEnabled(Boolean onlyOfficeEnabled) {
        ConvertDocConfigEngineOnlyOffice.onlyOfficeEnabled = onlyOfficeEnabled;
    }
    /**
     * only office服务路径
     * 必须存在
     */
    public static String onlyOfficeDomain;
    @Value("${convert.engine.onlyOffice.domain:}")
    public void setDocService(String onlyOfficeDomain) {
        ConvertDocConfigEngineOnlyOffice.onlyOfficeDomain = onlyOfficeDomain;
    }
    /**超时时间*/
    public static Integer onlyOfficeTimeout;
    @Value("${convert.engine.onlyOffice.timeout:}")
    public void setDocService(Integer onlyOfficeTimeout) {
        ConvertDocConfigEngineOnlyOffice.onlyOfficeTimeout = onlyOfficeTimeout;
    }
    /**
     * JWT令牌
     */
    public static String onlyOfficeSecret;
    @Value("${convert.engine.onlyOffice.secret:}")
    public void setSecret(String onlyOfficeSecret) {
        ConvertDocConfigEngineOnlyOffice.onlyOfficeSecret = onlyOfficeSecret;
    }
    /**
     * 是否异步处理。默认值：false
     */
    public static Boolean onlyOfficeAsync;
    @Value("${convert.engine.onlyOffice.async:false}")
    public void setAsync(Boolean onlyOfficeAsync) {
        ConvertDocConfigEngineOnlyOffice.onlyOfficeAsync = onlyOfficeAsync;
    }

}
