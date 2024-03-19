package com.thinkdifferent.convertpreview.engine.impl.localOffice;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.engine.impl.AbstractEngineServiceImpl;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 本地转换，优先级最低
 * 支持MS Office
 *
 * @author 张镇
 * @version 3.0
 * @date 2024/1/5 10:42
 */
@Service
@ConditionalOnProperty(name = "convert.engine.localUtil.office.enabled", havingValue = "true")
public class LocalMSOfficeEngineServiceImpl extends AbstractEngineServiceImpl {

    /**
     * MS Office引擎实现的PDF文件转换
     *
     * @param inputFile      输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @SneakyThrows
    @Override
    public File doConvertPdf(File inputFile, String strOutputFilePath){
        Assert.isTrue(LocalConvertDocUtil.process(FileUtil.getCanonicalPath(inputFile), strOutputFilePath),
                "本地MS Office引擎转换失败");
        return new File(strOutputFilePath);
    }

    /**
     * 该引擎支持的文件后缀
     *
     * @return list
     */
    @Override
    public List<String> supportFileExt() {
        return Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "vsd", "vsdx");
    }
}
