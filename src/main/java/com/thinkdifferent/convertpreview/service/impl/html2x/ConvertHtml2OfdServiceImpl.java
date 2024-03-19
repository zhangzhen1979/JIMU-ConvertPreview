package com.thinkdifferent.convertpreview.service.impl.html2x;

import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.service.impl.pdf2x.ConvertPdf2OfdServiceImpl;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 18:02
 */
@Service
public class ConvertHtml2OfdServiceImpl implements ConvertTypeService {
    @Resource(name="convertHtml2PdfServiceImpl")
    private ConvertHtml2PdfServiceImpl convertHtml2PdfService;
    @Resource(name="convertPdf2OfdServiceImpl")
    private ConvertPdf2OfdServiceImpl convertPdf2OfdService;
    /**
     * html转pdf
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        File pdf = convertHtml2PdfService.convert(inputFile, targetDir);
        return convertPdf2OfdService.convert(pdf, targetDir);
    }
}
