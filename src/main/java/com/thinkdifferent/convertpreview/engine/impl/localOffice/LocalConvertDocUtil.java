package com.thinkdifferent.convertpreview.engine.impl.localOffice;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.OsInfo;
import com.jacob.com.ComThread;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineLocal;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Log4j2
public class LocalConvertDocUtil {
    private static ExecutorService executorService = Executors.newFixedThreadPool(5);

    @PreDestroy
    public void preDestroy() {
        executorService.shutdown();
        if (ConvertDocConfigEngineLocal.wpsEnabled || ConvertDocConfigEngineLocal.officeEnabled) {
            ComThread.quitMainSTA();
        }
    }

    public static synchronized boolean process(String strOfficeFile, String strPdfFile) throws IOException {
        if (!checkfile(strOfficeFile)) {
            log.info(strOfficeFile + " is not file");
            return false;
        }

        if (ConvertDocConfigEngineLocal.wpsEnabled || ConvertDocConfigEngineLocal.officeEnabled) {
            strOfficeFile = SystemUtil.beautifulFilePath(strOfficeFile);
            String strInputFileType = FileUtil.extName(new File(strOfficeFile));

            strPdfFile = SystemUtil.beautifulFilePath(strPdfFile);
            File filePDF = new File(strPdfFile);
            FileUtil.touch(filePDF).delete();

            if (new OsInfo().isWindows()) {
                log.info("Windows开始");

                if (ConvertDocConfigEngineLocal.wpsEnabled || ConvertDocConfigEngineLocal.officeEnabled) {
                    int intReturn = -1;
                    if (!new File(strOfficeFile).exists()) {
                        intReturn = -2;
                    } else {
                        String strAppName = getUseAppName();

                        if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "doc", "docx", "txt", "rtf")) {
                            intReturn = DocByJacob.doc2PDF(strAppName, strOfficeFile, strPdfFile);
                        } else if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "ppt", "pptx")) {
                            intReturn = DocByJacob.ppt2PDF(strAppName, strOfficeFile, strPdfFile);
                        } else if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "xls", "xlsx", "csv")) {
                            intReturn = DocByJacob.xls2PDF(strAppName, strOfficeFile, strPdfFile);
                        } else if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "vsd", "vsdx")) {
                            intReturn = DocByJacob.vsd2PDF(strOfficeFile, strPdfFile);
                        } else {
                            intReturn = DocByJacob.doc2PDF(strAppName, strOfficeFile, strPdfFile);
                        }
                    }

                    if (intReturn == -2) {
                        log.info("Convert PDF by LocalUtil fail, Source File[" + strOfficeFile + "] is not exist...");
                        return false;
                    } else if (intReturn == -1) {
                        log.info("Convert PDF by LocalUtil fail, Please try again...");
                        return false;
                    } else {
                        log.info("Convert PDF by LocalUtil success, Use time is: " + intReturn + " s...");
                        return true;
                    }
                } else {
                    log.info("No Enable local tools in Windows, Convert Fail.");
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * 根据设置、输入的文件扩展名，判断到底使用那个本地引擎
     *
     * @return 使用的本地引擎名称：wps、office
     */
    private static String getUseAppName() {
        if (ConvertDocConfigEngineLocal.wpsEnabled) {
            return "wps";
        }

        if (ConvertDocConfigEngineLocal.officeEnabled) {
            return "office";
        }

        return "wps";
    }


    private static boolean checkfile(String strInputPath) {
        File file = new File(strInputPath);
        return file.isFile();
    }


}
