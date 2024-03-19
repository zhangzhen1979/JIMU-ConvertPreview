package com.thinkdifferent.convertpreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigEngineLibreOffice {

    /**
     * 转换引擎：本地Libre Office。是否开启。默认：否
     */
    public static Boolean libreOfficeEnabled;
    @Value("${jodconverter.local.enabled:false}")
    public void setLibreOfficeEnabled(Boolean libreOfficeEnabled) {
        ConvertDocConfigEngineLibreOffice.libreOfficeEnabled = libreOfficeEnabled;
    }
    /**
     * 转换引擎：本地Libre Office。程序根目录
     */
    public static String libreOfficePath;
    @Value("${jodconverter.local.office-home:Z:/LibreOffice}")
    public void setLibreOfficePath(String libreOfficePath) {
        ConvertDocConfigEngineLibreOffice.libreOfficePath = libreOfficePath;
    }
    /**
     * 转换引擎：本地Libre Office。端口（线程）
     */
    public static String libreOfficePortNumbers;
    @Value("${jodconverter.local.portNumbers:8001,8002,8003}")
    public void setLibreOfficePortNumbers(String libreOfficePortNumbers) {
        ConvertDocConfigEngineLibreOffice.libreOfficePortNumbers = libreOfficePortNumbers;
    }

}
