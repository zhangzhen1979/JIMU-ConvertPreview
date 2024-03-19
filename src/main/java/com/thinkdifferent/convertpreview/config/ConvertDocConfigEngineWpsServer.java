package com.thinkdifferent.convertpreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigEngineWpsServer {

     /**
     * 转换引擎：WPS预览服务。是否开启。默认：否
     */
    public static Boolean wpsPreviewEnabled;
    @Value("${convert.engine.wpsPreview.enabled:false}")
    public void setWpsPreviewEnabled(Boolean wpsPreviewEnabled) {
        ConvertDocConfigEngineWpsServer.wpsPreviewEnabled = wpsPreviewEnabled;
    }
    /**
     * 转换引擎：WPS预览服务。接受所有修订。默认：否
     */
    public static Boolean wpsPreviewAcceptAllRevisions;
    @Value("${convert.engine.wpsPreview.accept_all_revisions:false}")
    public void setWpsPreviewAcceptAllRevisions(Boolean wpsPreviewAcceptAllRevisions) {
        ConvertDocConfigEngineWpsServer.wpsPreviewAcceptAllRevisions = wpsPreviewAcceptAllRevisions;
    }
    /**
     * 转换引擎：WPS预览服务。删除所有批注。默认：否
     */
    public static Boolean wpsPreviewDeleteAllComments;
    @Value("${convert.engine.wpsPreview.delete_all_comments:false}")
    public void setWpsPreviewDeleteAllComments(Boolean wpsPreviewDeleteAllComments) {
        ConvertDocConfigEngineWpsServer.wpsPreviewDeleteAllComments = wpsPreviewDeleteAllComments;
    }
    /**
     * 转换引擎：WPS预览服务。删除所有墨迹。默认：否
     */
    public static Boolean wpsPreviewDeleteAllInk;
    @Value("${convert.engine.wpsPreview.delete_all_ink:false}")
    public void setWpsPreviewDeleteAllInk(Boolean wpsPreviewDeleteAllInk) {
        ConvertDocConfigEngineWpsServer.wpsPreviewDeleteAllInk = wpsPreviewDeleteAllInk;
    }

}
