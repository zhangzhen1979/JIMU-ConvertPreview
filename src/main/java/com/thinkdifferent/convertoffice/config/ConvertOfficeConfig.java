package com.thinkdifferent.convertoffice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class ConvertOfficeConfig {

    public static String inPutTempPath;
    @Value("${convert.office.inPutTempPath}")
    public void setInPutTempPath(String inPutTempPath) {
        ConvertOfficeConfig.inPutTempPath = inPutTempPath;
    }

    public static String outPutPath;
    @Value("${convert.office.outPutPath}")
    public void setOutPutPath(String outPutPath) {
        ConvertOfficeConfig.outPutPath = outPutPath;
    }

    public static String textWaterMarkType;
    @Value("${convert.watermark.text.type:static}")
    public void setTextWaterMarkType(String textWaterMarkType) {
        ConvertOfficeConfig.textWaterMarkType = textWaterMarkType;
    }

    public static String textWaterMarkContext;
    @Value("${convert.watermark.text.context:My Company}")
    public void setTextWaterMarkContext(String textWaterMarkContext) {
        ConvertOfficeConfig.textWaterMarkContext = textWaterMarkContext;
    }

}
