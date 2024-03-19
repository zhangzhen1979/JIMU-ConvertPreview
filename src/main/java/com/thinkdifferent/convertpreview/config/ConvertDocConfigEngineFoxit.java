package com.thinkdifferent.convertpreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigEngineFoxit {

    /**
     * 转换引擎：福昕转换服务。是否开启。默认：否
     */
    public static Boolean foxitEnabled;
    @Value("${convert.engine.foxit.enabled:false}")
    public void setFoxitEnabled(Boolean foxitEnabled) {
        ConvertDocConfigEngineFoxit.foxitEnabled = foxitEnabled;
    }
    /**
     * 福昕转换服务访问地址。
     */
    public static String foxitDomain;
    @Value("${convert.engine.foxit.domain:}")
    public void setFoxitDomain(String foxitDomain) {
        ConvertDocConfigEngineFoxit.foxitDomain = foxitDomain;
    }
    /**
     * 转换引擎：福昕转换服务。接受所有修订。默认：否
     */
    public static Boolean foxitAcceptRev;
    @Value("${convert.engine.foxit.acceptRev:false}")
    public void setFoxitAcceptRev(Boolean foxitAcceptRev) {
        ConvertDocConfigEngineFoxit.foxitAcceptRev = foxitAcceptRev;
    }
    /**
     * 转换引擎：福昕转换服务。删除所有批注。默认：否
     */
    public static Boolean foxitShowComments;
    @Value("${convert.engine.foxit.showComments:false}")
    public void setFoxitShowComments(Boolean foxitShowComments) {
        ConvertDocConfigEngineFoxit.foxitShowComments = foxitShowComments;
    }

}
