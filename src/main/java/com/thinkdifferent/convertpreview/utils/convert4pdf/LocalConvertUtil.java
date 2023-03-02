package com.thinkdifferent.convertpreview.utils.convert4pdf;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.OsInfo;
import com.jacob.com.ComThread;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.utils.FileTypeUtil;
import com.thinkdifferent.convertpreview.utils.PrintStream;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import com.thinkdifferent.convertpreview.utils.convert4pdf.jacob.ConvertByJacob;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Log4j2
public class LocalConvertUtil {
    private static ExecutorService executorService = Executors.newFixedThreadPool(5);

    @PreDestroy
    public void preDestroy() {
        executorService.shutdown();
        ComThread.quitMainSTA();
    }

    public static synchronized boolean process(String strOfficeFile, String strPdfFile) throws IOException, InterruptedException {
        if (!checkfile(strOfficeFile)) {
            log.info(strOfficeFile + " is not file");
            return false;
        }
        strOfficeFile = SystemUtil.beautifulFilePath(strOfficeFile);
        String strInputFileType = FileTypeUtil.getFileType(new File(strOfficeFile));

        strPdfFile = SystemUtil.beautifulFilePath(strPdfFile);
        File filePDF = new File(strPdfFile);
        FileUtil.touch(filePDF).delete();

        String strPath = SystemUtil.beautifulFilePath(System.getProperty("user.dir") + "/utils/");

        if (new OsInfo().isWindows()) {
            log.info("Windows开始");

            if (ConvertConfig.wpsEnabled || ConvertConfig.officeEnabled) {
                int intReturn = -1;
                if (!new File(strOfficeFile).exists()) {
                    intReturn = -2;
                } else {
                    String strAppName = getUseAppName(strInputFileType);

                    if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "doc", "docx", "txt", "rtf")) {
                        if ("jacob".equalsIgnoreCase(ConvertConfig.wpsRunType) || "jacob".equalsIgnoreCase(ConvertConfig.officeRunType)) {
                            intReturn = ConvertByJacob.doc2PDF(strAppName, strOfficeFile, strPdfFile);
                        } else if ("exe".equalsIgnoreCase(ConvertConfig.wpsRunType) || "exe".equalsIgnoreCase(ConvertConfig.officeRunType)) {
                            intReturn = runExe(strAppName, strPath, strInputFileType, strOfficeFile, strPdfFile);
                        }
                    } else if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "ppt", "pptx")) {
                        if ("jacob".equalsIgnoreCase(ConvertConfig.wpsRunType) || "jacob".equalsIgnoreCase(ConvertConfig.officeRunType)) {
                            intReturn = ConvertByJacob.ppt2PDF(strAppName, strOfficeFile, strPdfFile);
                        } else if ("exe".equalsIgnoreCase(ConvertConfig.wpsRunType) || "exe".equalsIgnoreCase(ConvertConfig.officeRunType)) {
                            intReturn = runExe(strAppName, strPath, strInputFileType, strOfficeFile, strPdfFile);
                        }
                    } else if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "xls", "xlsx", "csv")) {
                        if ("jacob".equalsIgnoreCase(ConvertConfig.wpsRunType) || "jacob".equalsIgnoreCase(ConvertConfig.officeRunType)) {
                            intReturn = ConvertByJacob.xls2PDF(strAppName, strOfficeFile, strPdfFile);
                        } else if ("exe".equalsIgnoreCase(ConvertConfig.wpsRunType) || "exe".equalsIgnoreCase(ConvertConfig.officeRunType)) {
                            intReturn = runExe(strAppName, strPath, strInputFileType, strOfficeFile, strPdfFile);
                        }
                    } else if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "vsd", "vsdx")) {
                        if ("jacob".equalsIgnoreCase(ConvertConfig.officeRunType)) {
                            intReturn = ConvertByJacob.vsd2PDF(strOfficeFile, strPdfFile);
                        } else if ("exe".equalsIgnoreCase(ConvertConfig.officeRunType)) {
                            intReturn = runExe(strAppName, strPath, strInputFileType, strOfficeFile, strPdfFile);
                        }
                    } else {
                        intReturn = -4;
                    }
                }

                if (intReturn == -4) {
                    log.info("Convert PDF fail, fileType not support ...");
                    return false;
                } else if (intReturn == -2) {
                    log.info("Convert PDF fail, Source File[" + strOfficeFile + "] is not exist...");
                    return false;
                } else if (intReturn == -1) {
                    log.info("Convert PDF fail, Please try again...");
                    return false;
                } else if (intReturn < -4) {
                    log.info("Convert PDF fail, Please try again...");
                    return false;
                } else {
                    log.info("Convert PDF success, Use time is: " + intReturn + " s...");
                }
            } else {
                log.info("No Enable local tools in Windows, Convert Fail.");
                return false;
            }

        } else {
            log.info("linux开始");
//            if("wps".equalsIgnoreCase(ConvertOfficeConfig.localUtil)){
//                strPath = strPath + "wps2pdf.exe";
//            }else if("office".equalsIgnoreCase(ConvertOfficeConfig.localUtil)){
//                strPath = strPath + "office2pdf.exe";
//            }else{
//                return false;
//            }

            strPath = strPath + " " + strOfficeFile + " " + strPdfFile;
            strPath = SystemUtil.beautifulFilePath(strPath);

            // 执行命令
            Process process = Runtime.getRuntime().exec(strPath);
            // 取得命令结果的输出流
            InputStream inputStream = process.getInputStream();
            // 用一个读输出流类去读
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            // 用缓冲器读行
            BufferedReader br = new BufferedReader(inputStreamReader);
            String strLine;
            // 直到读完为止
            while ((strLine = br.readLine()) != null) {
                log.info("文档转换:{}", strLine);
            }

            process.waitFor();
            process.destroy();

        }

        return true;
    }


    private static int runExe(String strAppName, String strPath, String strInputFileType,
                              String strOfficeFile, String strPdfFile) {
        try {
            Future<Integer> future = executorService.submit(() -> {
                try {
                    return runExe0(strAppName, strPath, strInputFileType, strOfficeFile, strPdfFile);
                } catch (Exception e) {
                    log.error("exe 转 pdf 异常", e);
                    return -1;
                }
            });
            return future.get();
        } catch (Exception e) {
            log.error("线程池执行exe转换获取结果异常", e);
            return -1;
        }

    }

    private static int runExe0(String strAppName, String strPath, String strInputFileType,
                               String strOfficeFile, String strPdfFile) throws InterruptedException, IOException {
        // 如果本地转换引擎启用，则判断是否使用对应的本地工具
        if ("wps".equalsIgnoreCase(strAppName)) {
            if (!StringUtils.isEmpty(ConvertConfig.wpsFile)) {
                // 如果配置了文件类型，则列表中的文件使用WPS转换
                String[] strsWpsFile = ConvertConfig.wpsFile.split(",");
                if (StringUtils.equalsAnyIgnoreCase(strInputFileType, strsWpsFile)) {
                    strPath = strPath + "wps2pdf.exe";
                }
            } else {
                // 如果没配置，则全部使用WPS转换
                strPath = strPath + "wps2pdf.exe";
            }

        } else if ("office".equalsIgnoreCase(strAppName)) {
            if (!StringUtils.isEmpty(ConvertConfig.officeFile)) {
                // 如果配置了文件类型，则列表中的文件使用Office转换
                String[] strsOfficeFile = ConvertConfig.officeFile.split(",");
                if (StringUtils.equalsAnyIgnoreCase(strInputFileType, strsOfficeFile)) {
                    strPath = strPath + "office2pdf.exe";
                }
            } else {
                // 如果没配置，则全部使用Office转换
                strPath = strPath + "office2pdf.exe";
            }

        } else {
            return -1;
        }


        if (!strPath.endsWith("2pdf.exe")) {
            return -1;
        }

        List<String> listCommand = new ArrayList<>();
        listCommand.add(strPath);
        listCommand.add(strOfficeFile);
        listCommand.add(strPdfFile);

        log.info("[process]Command:" + listCommand.toString());

        Process process = new ProcessBuilder(listCommand).redirectErrorStream(true).start();
        new PrintStream(process.getErrorStream()).start();
        new PrintStream(process.getInputStream()).start();
        process.waitFor();
        process.destroy();

        return 200;
    }

    /**
     * 根据设置、输入的文件扩展名，判断到底使用那个本地引擎
     *
     * @param strInputFileType 文件扩展名
     * @return 使用的本地引擎名称：wps、office
     */
    private static String getUseAppName(String strInputFileType) {
        if (ConvertConfig.wpsEnabled) {
            if (!StringUtils.isEmpty(ConvertConfig.wpsFile)) {
                // 如果配置了文件类型，则列表中的文件使用WPS转换
                String[] strsWpsFile = ConvertConfig.wpsFile.split(",");
                if (StringUtils.equalsAnyIgnoreCase(strInputFileType, strsWpsFile)) {
                    return "wps";
                }
            } else {
                // 如果没配置，则全部使用WPS转换
                return "wps";
            }
        }

        if (ConvertConfig.officeEnabled) {
            if (!StringUtils.isEmpty(ConvertConfig.officeFile)) {
                // 如果配置了文件类型，则列表中的文件使用Office转换
                String[] strsOfficeFile = ConvertConfig.officeFile.split(",");
                if (StringUtils.equalsAnyIgnoreCase(strInputFileType, strsOfficeFile)) {
                    return "office";
                }
            } else {
                // 如果没配置，则全部使用Office转换
                return "office";
            }
        }

        return "wps";
    }


    private static boolean checkfile(String strInputPath) {
        File file = new File(strInputPath);
        return file.isFile();
    }


}
