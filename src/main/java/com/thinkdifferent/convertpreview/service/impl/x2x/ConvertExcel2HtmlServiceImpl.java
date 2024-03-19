package com.thinkdifferent.convertpreview.service.impl.x2x;

import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.service.PoiConvertTypeService;
import com.thinkdifferent.convertpreview.utils.poi.Excel2HtmlUtil;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2024/1/4 10:24
 */
@Service
@ConditionalOnProperty(name = "convert.preview.poi.excel", havingValue = "true")
public class ConvertExcel2HtmlServiceImpl implements ConvertTypeService, PoiConvertTypeService {
    /**
     * 根据类型进行转换
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        String htmlFilePath = targetDir + ".html";
        Excel2HtmlUtil.excel2html(inputFile, htmlFilePath);
        return new File(htmlFilePath);
    }
}
