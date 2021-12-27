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


}
