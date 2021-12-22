package com.thinkdifferent.convertoffice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class ConvertOfficeConfig {

    @Value(value = "${convert.office.inPutTempPath}")
    public static String inPutTempPath;

    @Value(value = "${convert.office.outPutPath}")
    public static String outPutPath;

    @Value(value = "${convert.watermark.text.type:static}")
    public static String textWaterMarkType;

    @Value(value = "${convert.watermark.text.context:My Company}")
    public static String textWaterMarkContext;
}
