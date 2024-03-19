package com.thinkdifferent.convertpreview.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigPreview {

    /**
     * 默认的预览模式。img/pdf。默认值：pdf
     * 优先级比请求中传入的参数低
     */
    public static String previewType;
    @Value("${convert.preview.type:pdf}")
    public void setPreviewType(String previewType) {
        ConvertDocConfigPreview.previewType = StringUtils.equalsIgnoreCase(previewType, "pdf") ? "pdf" : "officePicture";
    }

    /**
     * 是否可切换预览模式。默认值：true
     */
    public static String blnChangeType;
    @Value("${convert.preview.blnChange:true}")
    public void setBlnChangeType(boolean blnChangeType) {
        ConvertDocConfigPreview.blnChangeType = blnChangeType ? "true" : "false";
    }

    /**
     * 是否启用预览界面水印。默认值：true
     */
    public static Boolean watermarkEnable;
    @Value("${convert.preview.watermarkEnable:true}")
    public void setWatermarkEnable(Boolean watermarkEnable) {
        ConvertDocConfigPreview.watermarkEnable = watermarkEnable;
    }
    public static String watermarkText;
    @Value("${convert.preview.watermarkTxt:}")
    public void setWatermarkText(String watermarkText){
        ConvertDocConfigPreview.watermarkText = watermarkText;
    }

    public static String watermarkImage;
    @Value("${convert.preview.watermarkImage:}")
    public void setWatermarkImage(String watermarkImage){
        ConvertDocConfigPreview.watermarkImage = watermarkImage;
    }

    /**
     * 异步转换的图片数量，
     * 默认：0 (不进行异步加载)
     * 1：加载1页，其他页异步加载
     */
    public static Integer asyncImgNum;

    @Value("${convert.preview.asyncImgNum:0}")
    public void setAsyncImgNum(Integer asyncImgNum) {
        ConvertDocConfigPreview.asyncImgNum = asyncImgNum;
    }


    public static Boolean useTicket;
    @Value("${convert.preview.useTicket:false}")
    public void setUseTicket(Boolean useTicket){
        ConvertDocConfigPreview.useTicket = useTicket;
    }

    public static Boolean timeControl;
    @Value("${convert.preview.timeControl:false}")
    public void setTimeControl(Boolean timeControl){
        ConvertDocConfigPreview.timeControl = timeControl;
    }


}
