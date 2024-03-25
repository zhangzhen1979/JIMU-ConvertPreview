package com.thinkdifferent.convertpreview.service.impl.x2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.service.impl.pdf2x.ConvertPdf2JpgServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

/**
 * xbrl 预览，其他xml返回原始文件
 *
 * @author ltian
 * @version 1.0
 * @date 2024/1/4 10:24
 */
@Log4j2
@Service
public class ConvertXml2JpgServiceImpl implements ConvertTypeService {
    @Resource(name = "convertXml2PdfServiceImpl")
    private ConvertXml2PdfServiceImpl convertXml2PdfService;
    @Resource(name = "convertPdf2JpgServiceImpl")
    private ConvertPdf2JpgServiceImpl convertPdf2JpgService;

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
        // 读取文件根节点，判断是否为xbrl。如果是，则走【xml-json-报表】的方式预览。
        File pdfFile = convertXml2PdfService.convert(inputFile, targetDir);
        if ("pdf".equalsIgnoreCase(FileUtil.extName(pdfFile))) {
            return convertPdf2JpgService.convert(pdfFile, targetDir);
        }
        // 非xbrl，直接返回
        return inputFile;
    }
}
