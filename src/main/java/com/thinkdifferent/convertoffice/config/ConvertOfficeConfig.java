package com.thinkdifferent.convertoffice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConvertOfficeConfig {

    public static String inPutTempPath;

    @Value("${convert.office.inPutTempPath}")
    public void setInPutTempPath(String strInPutTempPath) {
        ConvertOfficeConfig.inPutTempPath = strInPutTempPath;
    }

    public static String outPutPath;

    @Value("${convert.office.outPutPath}")
    public void setOutPutPath(String strOutPutPath) {
        ConvertOfficeConfig.outPutPath = strOutPutPath;
    }

}
