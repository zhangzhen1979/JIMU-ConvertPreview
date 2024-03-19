package com.thinkdifferent.convertpreview.service.impl.ppt2x;


import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * ppt 转 pdf
 *
 * @author ltian
 * @version 1.0
 * @date 2024/1/22 19:08
 */
@Log4j2
@Service
@ConditionalOnProperty(name = "convert.preview.poi.ppt", havingValue = "true")
public class ConvertPpt2PdfServiceImpl implements ConvertTypeService {
    @Resource(name = "convertPpt2JpgServiceImpl")
    private ConvertTypeService convertTypeService;

    /**
     * 根据类型进行转换, 内部实现，不建议直接调用
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        File fileJpgPath = convertTypeService.convert(inputFile, targetDir);
        // 需要转换的jpg文件
        File[] jpgFiles;
        if (fileJpgPath.isFile()) {
            jpgFiles = new File[]{fileJpgPath};
        } else {
            // 目录读取目录下文件夹
            jpgFiles = fileJpgPath.listFiles();
        }

        // 再转 pdf
        String pdfFile = targetDir + ".pdf";
        @Cleanup PDDocument pdDocument = new PDDocument();

        for (File fileJpg : jpgFiles) {
            if (fileJpg.exists()) {
                BufferedImage image = ImageIO.read(fileJpg);
                PDImageXObject pdImageXObject = LosslessFactory.createFromImage(pdDocument, image);
                PDPage pdPage = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                pdDocument.addPage(pdPage);
                @Cleanup PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage);
                contentStream.drawImage(pdImageXObject, 0, 0, image.getWidth(), image.getHeight());
            }
        }
        pdDocument.save(pdfFile);
        return new File(pdfFile);
    }

}
