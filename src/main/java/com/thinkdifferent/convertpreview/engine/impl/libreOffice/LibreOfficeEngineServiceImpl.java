package com.thinkdifferent.convertpreview.engine.impl.libreOffice;

import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineLibreOffice;
import com.thinkdifferent.convertpreview.engine.impl.AbstractEngineServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 本地LibreOffice引擎
 *
 * @author 张镇
 * @version 3.0
 * @date 2024/1/5 10:42
 */
@Service
@ConditionalOnProperty(name = "jodconverter.local.enabled", havingValue = "true")
@Log4j2
public class LibreOfficeEngineServiceImpl extends AbstractEngineServiceImpl {

    /**
     * 本地LibreOffice实现的文件转换
     *
     * @param inputFile         输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @SneakyThrows
    @Override
    public File doConvertPdf(File inputFile, String strOutputFilePath) {
        File fileOutput = new File(strOutputFilePath);

        String[] strPorts = ConvertDocConfigEngineLibreOffice.libreOfficePortNumbers.split(",");
        int[] intPorts = new int[strPorts.length];
        for (int i = 0; i < intPorts.length; i++) {
            intPorts[i] = Integer.parseInt(strPorts[i]);
        }

        OfficeManager officeManager = LocalOfficeManager.builder()
                .officeHome(ConvertDocConfigEngineLibreOffice.libreOfficePath)
                .portNumbers(intPorts)
                .build();

        try {
            officeManager.start();
            DocumentConverter converter = LocalConverter.builder()
                    .officeManager(officeManager)
                    .build();

            converter.convert(inputFile).to(fileOutput).execute();
        } catch (Exception | Error e) {
            e.printStackTrace();
        } finally {
            officeManager.stop();
        }

        return fileOutput;
    }

    /**
     * 该引擎支持的文件后缀
     *
     * @return list
     */
    @Override
    public List<String> supportFileExt() {
        return Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv");
    }
}
