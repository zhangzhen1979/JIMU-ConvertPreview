package com.thinkdifferent.convertpreview.config;

import com.thinkdifferent.convertpreview.utils.SystemUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class ConvertConfig {

    public static String inPutTempPath;
    @Value("${convert.path.inPutTempPath:}")
    public void setInPutTempPath(String inPutTempPath) {
        ConvertConfig.inPutTempPath = inPutTempPath;
    }

    public static String outPutPath;
    @Value("${convert.path.outPutPath:}")
    public void setOutPutPath(String outPutPath) {
        ConvertConfig.outPutPath = SystemUtil.beautifulDir(outPutPath);
    }

    public static String picType;
    @Value("${convert.engine.picType:}")
    public void setPicType(String picType) {
        ConvertConfig.picType = picType;
    }

    public static Boolean wpsEnabled;
    @Value("${convert.engine.localUtil.wps.enabled:}")
    public void setWpsEnabled(Boolean wpsEnabled) {
        ConvertConfig.wpsEnabled = wpsEnabled;
    }

    public static String wpsFile;
    @Value("${convert.engine.localUtil.wps.fileType:}")
    public void setWpsFile(String wpsFile) {
        ConvertConfig.wpsFile = wpsFile;
    }

    public static String wpsRunType;
    @Value("${convert.engine.localUtil.wps.runType:}")
    public void setWpsRunType(String wpsRunType) {
        ConvertConfig.wpsRunType = wpsRunType;
    }

    public static Boolean officeEnabled;
    @Value("${convert.engine.localUtil.office.enabled:}")
    public void setOfficeEnabled(Boolean officeEnabled) {
        ConvertConfig.officeEnabled = officeEnabled;
    }

    public static String officeFile;
    @Value("${convert.engine.localUtil.office.fileType:}")
    public void setOfficeFile(String officeFile) {
        ConvertConfig.officeFile = officeFile;
    }

    public static String officeRunType;
    @Value("${convert.engine.localUtil.office.runType:}")
    public void setOfficeRunType(String officeRunType) {
        ConvertConfig.officeRunType = officeRunType;
    }

    public static Boolean libreOfficeEnabled;
    @Value("${jodconverter.local.enabled:}")
    public void setLibreOfficeEnabled(Boolean libreOfficeEnabled) {
        ConvertConfig.libreOfficeEnabled = libreOfficeEnabled;
    }

}
