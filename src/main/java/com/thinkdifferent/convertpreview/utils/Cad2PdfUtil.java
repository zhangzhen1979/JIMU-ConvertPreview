package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.OsInfo;
import com.jacob.com.ComThread;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineBase;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Log4j2
public class Cad2PdfUtil {
    private static ExecutorService executorService = Executors.newFixedThreadPool(5);

    @PreDestroy
    public void preDestroy() {
        executorService.shutdown();
        ComThread.quitMainSTA();
    }

    public static synchronized boolean process(String strCadFile, String strPdfFile) throws IOException {
        if (!checkfile(strCadFile)) {
            log.info(strCadFile + " is not file");
            return false;
        }
        strCadFile = SystemUtil.beautifulFilePath(strCadFile);
        // 获取文件扩展名。目前此程序可以处理dwg、dxf格式
        String strInputFileType = FileUtil.extName(new File(strCadFile));
        if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "dwg", "dxf")) {
            strPdfFile = SystemUtil.beautifulFilePath(strPdfFile);
            File filePDF = new File(strPdfFile);
            FileUtil.touch(filePDF).delete();


            String strPath = ConvertDocConfigEngineBase.cadUtilPath;

            if (new OsInfo().isWindows()) {
                log.info("Windows转换CAD文件开始");
                strPath = strPath + "dwg2pdf.bat";
            } else {
                log.info("Linux转换CAD文件开始");
                strPath = strPath + "dwg2pdf";
            }
            long date = new Date().getTime();


            int intReturn = -1;
            if (!new File(strCadFile).exists()) {
                // 源文件不存在，直接返回状态值：-2
                intReturn = -2;
            } else {
                intReturn = runExe(strPath, strCadFile, strPdfFile);
            }
            long date2 = new Date().getTime();

            if (intReturn == -4) {
                log.info("CAD Convert PDF fail, fileType not support ...");
                return false;
            } else if (intReturn == -2) {
                log.info("CAD Convert PDF fail, Source File[" + strCadFile + "] is not exist...");
                return false;
            } else if (intReturn == -1) {
                log.info("CAD Convert PDF fail, Please try again...");
                return false;
            } else if (intReturn < -4) {
                log.info("CAD Convert PDF fail, Please try again...");
                return false;
            } else {
                log.info("CAD Convert PDF success, Use time is: " + (date2 - date) + " ms...");
            }
        } else {
            return false;
        }

        return true;
    }


    private static int runExe(String strPath,
                              String strCadFile, String strPdfFile) {
        try {
            Future<Integer> future = executorService.submit(() -> {
                try {
                    return runExe0(strPath, strCadFile, strPdfFile);
                } catch (Exception | Error e) {
                    log.error("dwg 转 pdf 异常", e);
                    return -1;
                }
            });
            return future.get();
        } catch (Exception | Error e) {
            log.error("线程池执行exe转换获取结果异常", e);
            return -1;
        }

    }

    private static int runExe0(String strPath,
                               String strCadFile, String strPdfFile) throws InterruptedException, IOException {
        // dwg2pdf.bat -a -f -auto-orientation -outfile=c:\java\001.pdf 001.dwg
        List<String> listCommand = new ArrayList<>();
        listCommand.add(strPath);
        listCommand.add("-a");
        listCommand.add("-f");
        listCommand.add("-auto-orientation");
        listCommand.add("-outfile=" + strPdfFile);
        listCommand.add(strCadFile);

        log.info("[process]Command:" + listCommand.toString());

        Process process = new ProcessBuilder(listCommand).redirectErrorStream(true).start();
        new PrintStream(process.getErrorStream()).start();
        new PrintStream(process.getInputStream()).start();
        process.waitFor();
        process.destroy();

        return 200;
    }

    private static boolean checkfile(String strInputPath) {
        File file = new File(strInputPath);
        return file.isFile();
    }

}
