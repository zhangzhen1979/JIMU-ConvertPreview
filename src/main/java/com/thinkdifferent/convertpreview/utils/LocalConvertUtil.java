package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.OsInfo;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Log4j2
public class LocalConvertUtil {

    public static boolean process(String strOfficeFile, String strPdfFile) throws IOException, InterruptedException {
        if (!checkfile(strOfficeFile)) {
            log.info(strOfficeFile + " is not file");
            return false;
        }
        strOfficeFile = SystemUtil.beautifulFilePath(strOfficeFile);
        String strInputFileType = FileTypeUtil.getFileType(new File(strOfficeFile));

        strPdfFile = SystemUtil.beautifulFilePath(strPdfFile);
        File filePDF = new File(strPdfFile);
        if (filePDF.exists()) {
            filePDF.delete();
        } else {
            FileUtil.touch(filePDF);
        }

        String strPath = SystemUtil.beautifulFilePath(System.getProperty("user.dir") + "/utils/");

        if (new OsInfo().isWindows()) {
            log.info("Windows开始");

            if (ConvertConfig.wpsEnabled || ConvertConfig.officeEnabled) {
                int intReturn = -1;
                if (!new File(strOfficeFile).exists()) {
                    intReturn = -2;
                } else {
                    String strAppName = getUseAppName(strInputFileType);

                    if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "doc", "docx", "txt")) {
                        if("jacob".equalsIgnoreCase(ConvertConfig.wpsRunType) || "jacob".equalsIgnoreCase(ConvertConfig.officeRunType)){
                            intReturn = doc2PDF(strAppName, strOfficeFile, strPdfFile);
                        }else if("exe".equalsIgnoreCase(ConvertConfig.wpsRunType) || "exe".equalsIgnoreCase(ConvertConfig.officeRunType)){
                            intReturn = runExe(strAppName, strPath, strInputFileType, strOfficeFile, strPdfFile);
                        }
                    } else if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "ppt", "pptx")) {
                        if("jacob".equalsIgnoreCase(ConvertConfig.wpsRunType) || "jacob".equalsIgnoreCase(ConvertConfig.officeRunType)){
                            intReturn = ppt2PDF(strAppName, strOfficeFile, strPdfFile);
                        }else if("exe".equalsIgnoreCase(ConvertConfig.wpsRunType) || "exe".equalsIgnoreCase(ConvertConfig.officeRunType)){
                            intReturn = runExe(strAppName, strPath, strInputFileType, strOfficeFile, strPdfFile);
                        }
                    } else if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "xls", "xlsx")) {
                        if("jacob".equalsIgnoreCase(ConvertConfig.wpsRunType) || "jacob".equalsIgnoreCase(ConvertConfig.officeRunType)){
                            intReturn = xls2PDF(strAppName, strOfficeFile, strPdfFile);
                        }else if("exe".equalsIgnoreCase(ConvertConfig.wpsRunType) || "exe".equalsIgnoreCase(ConvertConfig.officeRunType)){
                            intReturn = runExe(strAppName, strPath, strInputFileType, strOfficeFile, strPdfFile);
                        }
                    } else if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "vsd", "vsdx")) {
                        if("jacob".equalsIgnoreCase(ConvertConfig.officeRunType)){
                            intReturn = vsd2PDF(strOfficeFile, strPdfFile);
                        }else if("exe".equalsIgnoreCase(ConvertConfig.officeRunType)){
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


    /***
     *
     * Word转PDF（Jacob）
     *
     * @param appName     应用名称：wps、office
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */

    private static int doc2PDF(String appName, String inputFile, String pdfFile) {
        ActiveXComponent app = null;
        Dispatch word = null;
        try {
            //  这句是调用初始化并放入内存中等待调用
            ComThread.InitSTA();
            // 打开WPS或Word应用程序
            if ("wps".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("KWPS.Application");
            } else if ("office".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("Word.Application");
            }
            log.info("开始转化Word为PDF...");
            long date = new Date().getTime();
            // 设置Word不可见
            app.setProperty("Visible", new Variant(false));
            app.setProperty("DisplayAlerts", new Variant(false));
            // 禁用宏
            app.setProperty("AutomationSecurity", new Variant(3));
            // 获得Word中所有打开的文档，返回documents对象
            Dispatch docs = app.getProperty("Documents").toDispatch();
            // 调用Documents对象中Open方法打开文档，并返回打开的文档对象Document
            word = Dispatch.call(docs, "Open", inputFile, false, true).toDispatch();
            /***
             *
             * 调用Document对象的SaveAs方法，将文档保存为pdf格式
             *
             * Dispatch.call(doc, "SaveAs", pdfFile, wdFormatPDF
             * word保存为pdf格式宏，值为17 )
             *
             */
            Dispatch.call(word, "ExportAsFixedFormat", pdfFile, 17);// word保存为pdf格式宏，值为17
            log.info(word);
            // 关闭文档
            long date2 = new Date().getTime();
            return (int) ((date2 - date) / 1000);
        } catch (Exception e) {
            log.error("本地应用转pdf异常", e);
            return -1;
        } finally {
            if (Objects.nonNull(word)) {
                Dispatch.call(word, "Close", false);
            }
            if (Objects.nonNull(app)) {
                app.invoke("Quit", 0);
            }
            // 释放占用的内存空间
            ComThread.Release();
        }

    }

    /***
     *
     * Excel转化成PDF(Jacob)
     *
     * @param appName     应用名称：wps、office
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */
    private static int xls2PDF(String appName, String inputFile, String pdfFile) {
        ActiveXComponent app = null;
        Dispatch excel = null;
        try {
            //  这句是调用初始化并放入内存中等待调用
            ComThread.InitSTA();
            // 打开WPS或Excel应用程序
            if ("wps".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("KET.Application");
            } else if ("office".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("Excel.Application");
            }
            log.info("开始转化Excel为PDF...");
            long date = new Date().getTime();
            app.setProperty("Visible", false);
            app.setProperty("DisplayAlerts", new Variant(false));
            app.setProperty("AutomationSecurity", new Variant(3)); // 禁用宏
            Dispatch excels = app.getProperty("Workbooks").toDispatch();

            excel = Dispatch
                    .invoke(excels, "Open", Dispatch.Method,
                            new Object[]{inputFile, new Variant(false), new Variant(false)}, new int[9])
                    .toDispatch();
            // 转换格式
            Dispatch.invoke(excel, "ExportAsFixedFormat", Dispatch.Method, new Object[]{new Variant(0), // PDF格式=0
                    pdfFile, new Variant(0) // 0=标准 (生成的PDF图片不会变模糊) 1=最小文件
                    // (生成的PDF图片糊的一塌糊涂)
            }, new int[1]);

            // 这里放弃使用SaveAs
            /*
             * Dispatch.invoke(excel,"SaveAs",Dispatch.Method,new Object[]{
             * outFile, new Variant(57), new Variant(false), new Variant(57),
             * new Variant(57), new Variant(false), new Variant(true), new
             * Variant(57), new Variant(true), new Variant(true), new
             * Variant(true) },new int[1]);
             */
            long date2 = new Date().getTime();
            return (int) ((date2 - date) / 1000);
        } catch (Exception e) {
            log.error("wps excel转pdf异常", e);
            return -1;
        } finally {
            if (Objects.nonNull(excel)) {
                Dispatch.call(excel, "Close", new Variant(false));
            }

            if (app != null) {
                app.invoke("Quit", new Variant[]{});
            }
            // 释放占用的内存空间
            ComThread.Release();
        }
    }

    /***
     * ppt转化成PDF
     *
     * @param appName     应用名称：wps、office
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */
    private static int ppt2PDF(String appName, String inputFile, String pdfFile) {
        ActiveXComponent app = null;
        Dispatch ppt = null;
        try {
            //  这句是调用初始化并放入内存中等待调用
            ComThread.InitSTA();
            // 打开WPS或PPT应用程序
            if ("wps".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("KWPP.Application");
            } else if ("office".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("PowerPoint.Application");
            }
            app.setProperty("DisplayAlerts", new Variant(false));

            log.info("开始转化PPT为PDF...");
            long date = new Date().getTime();

            Dispatch ppts = app.getProperty("Presentations").toDispatch();
            ppt = Dispatch.call(ppts, "Open", inputFile, true, // ReadOnly
                    //    false, // Untitled指定文件是否有标题
                    false// WithWindow指定文件是否可见
            ).toDispatch();

            pdfFile = pdfFile.replaceAll("/", "\\\\");
            Dispatch.invoke(ppt,
                    "SaveAs",
                    Dispatch.Method,
                    new Object[]{pdfFile, new Variant(32)},
                    new int[1]);

            long date2 = new Date().getTime();
            return (int) ((date2 - date) / 1000);
        } catch (Exception e) {
            log.error("ppt 转 pdf 异常", e);
            return -1;
        } finally {
            if (Objects.nonNull(ppt)) {
                Dispatch.call(ppt, "Close");
            }
            if (Objects.nonNull(app)) {
                app.invoke("Quit");
            }
            // 释放占用的内存空间
            ComThread.Release();
        }
    }


    /***
     * vsd（Visio））转化成PDF（只支持Office，Jacob方式调用）
     *
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */
    private static int vsd2PDF(String inputFile, String pdfFile) {
        ActiveXComponent app = null;
        ActiveXComponent documents = null;
        Dispatch vsd = null;
        try {
            //  这句是调用初始化并放入内存中等待调用
            ComThread.InitSTA();

            File fileInput = new File(inputFile);

            // 打开Office应用程序
            app = new ActiveXComponent("Visio.Application");
            app.setProperty("Visible", new Variant(false));

            documents = new ActiveXComponent(app.getProperty("Documents").toDispatch());

            log.info("开始转化VSD为PDF...");
            long date = new Date().getTime();

            vsd = documents.invoke("Open", new Variant(fileInput.getPath())).toDispatch();

            Dispatch.invoke(vsd,
                    "ExportAsFixedFormat",
                    Dispatch.Method,
                    new Object[]{new Variant(1), pdfFile, new Variant(1), new Variant(0)},
                    new int[1]);

            long date2 = new Date().getTime();
            return (int) ((date2 - date) / 1000);
        } catch (Exception e) {
            log.error("vsd 转 pdf 异常", e);
            return -1;
        } finally {
            if (Objects.nonNull(vsd)) {
                Dispatch.call(vsd, "Close");
            }
            if (Objects.nonNull(app)) {
                app.invoke("Quit");
            }
            // 释放占用的内存空间
            ComThread.Release();
        }
    }


}
