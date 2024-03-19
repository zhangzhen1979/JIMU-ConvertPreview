package com.thinkdifferent.convertpreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigEngineSuwell {

    /**
     * 转换引擎：数科转换服务。是否开启。默认：否
     */
    public static Boolean suwellEnabled;
    @Value("${convert.engine.suwell.enabled:false}")
    public void setSuwellEnabled(Boolean suwellEnabled) {
        ConvertDocConfigEngineSuwell.suwellEnabled = suwellEnabled;
    }
    /**
     * 数科转换服务访问地址。
     */
    public static String suwellDomain;
    @Value("${convert.engine.suwell.domain:}")
    public void setSuwellDomain(String suwellDomain) {
        ConvertDocConfigEngineSuwell.suwellDomain = suwellDomain;
    }

}
