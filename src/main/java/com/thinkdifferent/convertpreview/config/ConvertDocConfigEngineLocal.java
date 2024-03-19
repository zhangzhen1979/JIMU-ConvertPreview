package com.thinkdifferent.convertpreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigEngineLocal {

    /**
     * 本地转换引擎：接受所有修订。默认：否
     */
    public static Boolean localAcceptRevisions;
    @Value("${convert.engine.localUtil.acceptRevisions:false}")
    public void setLocalAcceptRevisions(Boolean localAcceptRevisions) {
        ConvertDocConfigEngineLocal.localAcceptRevisions = localAcceptRevisions;
    }
    /**
     * 本地转换引擎：关闭批注。默认：否
     */
    public static Boolean localDeleteComments;
    @Value("${convert.engine.localUtil.deleteComments:false}")
    public void setLocalDeleteComments(Boolean localDeleteComments) {
        ConvertDocConfigEngineLocal.localDeleteComments = localDeleteComments;
    }


    /**
     * 转换引擎：本地WPS Office。是否开启。默认：否
     */
    public static Boolean wpsEnabled;
    @Value("${convert.engine.localUtil.wps.enabled:false}")
    public void setWpsEnabled(Boolean wpsEnabled) {
        ConvertDocConfigEngineLocal.wpsEnabled = wpsEnabled;
    }
    /**
     * 转换引擎：本地Office。是否开启。默认：否
     */
    public static Boolean officeEnabled;
    @Value("${convert.engine.localUtil.office.enabled:false}")
    public void setOfficeEnabled(Boolean officeEnabled) {
        ConvertDocConfigEngineLocal.officeEnabled = officeEnabled;
    }

}
