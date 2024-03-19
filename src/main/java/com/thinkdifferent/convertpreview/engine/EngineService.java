package com.thinkdifferent.convertpreview.engine;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.util.List;

/**
 * 各引擎需实现各自的功能, 每次只能启用一个引擎
 *
 * @author 张镇
 * @version 3.0
 * @date 2024/1/5 10:42
 */
public interface EngineService {
    /**
     * 调用引擎进行文件转换，自动进行格式支持判断
     *
     * @param inputFile      输入文件
     * @param outputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    default File convertPdf(File inputFile, String outputFilePath) {
        String inputExtName = FileUtil.extName(inputFile);
        Assert.isTrue(supportFileExt().contains(inputExtName),
                "引擎" + this.getClass().getName() + "暂不支持的文件格式:" + inputExtName);
        return doConvertPdf(inputFile, outputFilePath);
    }

    /**
     * 引擎实现的pdf文件转换
     *
     * @param inputFile      输入文件
     * @param outputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    File doConvertPdf(File inputFile, String outputFilePath);

    /**
     * 调用引擎进行文件转换，自动进行格式支持判断
     *
     * @param inputFile      输入文件
     * @param outputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    default File convertOfd(File inputFile, String outputFilePath) {
        String inputExtName = FileUtil.extName(inputFile);
        Assert.isTrue(supportFileExt().contains(inputExtName),
                "引擎" + this.getClass().getName() + "暂不支持的文件格式:" + inputExtName);
        return doConvertOfd(inputFile, outputFilePath);
    }

    /**
     * 引擎实现的ofd文件转换
     *
     * @param inputFile      输入文件
     * @param outputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    default File doConvertOfd(File inputFile, String outputFilePath) {
        // 不支持直接转换ofd, 需先转pdf,再转ofd
        doConvertPdf(inputFile, outputFilePath + ".pdf");
        ConvertTypeService pdf2OfdServiceImpl = SpringUtil.getBean("convertPdf2OfdServiceImpl");
        return pdf2OfdServiceImpl.convert(new File(outputFilePath + ".pdf"),
                // 不需要后缀
                StringUtils.substring(outputFilePath, 0, outputFilePath.lastIndexOf(".")));
    }

    /**
     * 该引擎支持的文件后缀
     *
     * @return list
     */
    List<String> supportFileExt();
}
