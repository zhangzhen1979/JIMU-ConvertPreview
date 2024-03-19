package com.thinkdifferent.convertpreview.service.impl.cad2x;

import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.service.impl.pdf2x.ConvertPdf2OfdServiceImpl;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2024/1/3 18:21
 */
@Service
public class ConvertCad2OfdServiceImpl implements ConvertTypeService {

    @Resource(name = "convertCad2PdfServiceImpl")
    private ConvertCad2PdfServiceImpl convertCad2PdfService;
    @Resource(name = "convertPdf2OfdServiceImpl")
    private ConvertPdf2OfdServiceImpl convertPdf2OfdService;

    /**
     * cad 转 ofd
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        File pdf = convertCad2PdfService.convert(inputFile, targetDir);
        return convertPdf2OfdService.convert(pdf, targetDir);
    }
}
