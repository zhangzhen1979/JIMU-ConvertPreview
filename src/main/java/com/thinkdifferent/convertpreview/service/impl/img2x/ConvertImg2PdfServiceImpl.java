package com.thinkdifferent.convertpreview.service.impl.img2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.utils.OnlineCacheUtil;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 18:02
 */
@Log4j2
@Service
public class ConvertImg2PdfServiceImpl implements ConvertTypeService {
    @Resource(name = "convertImg2JpgServiceImpl")
    private ConvertImg2JpgServiceImpl convertImg2JpgService;

    /**
     * 图片转pdf
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        // 先转 jpg
        File fileJpgPath = convertImg2JpgService.convert(inputFile, targetDir + "_jpg");
        File fileJpgFile = new File(targetDir + "_jpg.jpg");

        // 需要转换的jpg文件
        File[] jpgFiles;
        if (fileJpgFile.isFile()) {
            jpgFiles = new File[]{fileJpgPath};
        } else if (fileJpgPath.isFile()) {
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

        // 删除临时文件夹中的jpg和目录

        OnlineCacheUtil.putTemp(inputFile, "jpg", FileUtil.file(pdfFile));
        return new File(pdfFile);
    }
}
