package com.thinkdifferent.convertpreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigEngineYzFCS {

    /**
     * 转换引擎：永中FCS预览服务。是否开启。默认：否
     */
    public static Boolean fcsEnabled;
    @Value("${convert.engine.fcs.enabled:false}")
    public void setFcsEnabled(Boolean fcsEnabled) {
        ConvertDocConfigEngineYzFCS.fcsEnabled = fcsEnabled;
    }
    /**
     * 永中FCS预览服务访问地址。
     */
    public static String fcsDomain;
    @Value("${convert.engine.fcs.domain:}")
    public void setFcsDomain(String fcsDomain) {
        ConvertDocConfigEngineYzFCS.fcsDomain = fcsDomain;
    }
    /**
     * 转换引擎：永中FCS接受修订记录。默认：否
     */
    public static Boolean fcsAcceptTracks;
    @Value("${convert.engine.fcs.acceptTracks:false}")
    public void setFcsAcceptTracks(Boolean fcsAcceptTracks) {
        ConvertDocConfigEngineYzFCS.fcsAcceptTracks = fcsAcceptTracks;
    }

}
