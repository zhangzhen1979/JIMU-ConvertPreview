package com.thinkdifferent.convertpreview.utils.convert4pdf;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.entity.OutFileEncryptorEntity;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF转换工具。
 * 各种格式转换为PDF文件。
 * PDF文件的处理（获取首页图等）
 */
@Order
@Component
@Log4j2
public class ConvertPdfUtil {

    /**
     * PDF加密，设置权限
     *
     * @param strPdfFilePath           输出的pdf文件路径和文件名
     * @param outFileEncryptorEntity   输出文件加密对象
     * @return
     * @throws IOException
     */
    public File pdfEncry(String strPdfFilePath, OutFileEncryptorEntity outFileEncryptorEntity) throws IOException {
        File filePdf = new File(strPdfFilePath);
        String strPdfEnc = strPdfFilePath + "_enc.pdf";

        if(outFileEncryptorEntity != null && outFileEncryptorEntity.getEncry()) {
            File fileSource = new File(strPdfFilePath);

            @Cleanup PDDocument pdDocument = Loader.loadPDF(fileSource);
            // 读取加密的PDF，需要传入密码
//            @Cleanup PDDocument pdDocument = Loader.loadPDF(fileSource, "123");

            AccessPermission permissions = new AccessPermission();
            // 是否可以插入/删除/旋转页面
            permissions.setCanAssembleDocument(outFileEncryptorEntity.getAssembleDocument());
            // 是否可以复制和提取内容
            permissions.setCanExtractContent(outFileEncryptorEntity.getCopy());

            permissions.setCanExtractForAccessibility(false);
            // 设置用户是否可以填写交互式表单字段（包括签名字段）
            permissions.setCanFillInForm(outFileEncryptorEntity.getFillInForm());
            // 设置用户是否可以修改文档
            permissions.setCanModify(outFileEncryptorEntity.getModify());
            // 设置用户是否可以添加或修改文本注释并填写交互式表单字段，如果canModify()返回true，则创建或修改交互式表单字段（包括签名字段）。
            permissions.setCanModifyAnnotations(outFileEncryptorEntity.getModifyAnnotations());
            // 设置用户是否可以打印。
            permissions.setCanPrint(outFileEncryptorEntity.getPrint());
            // 设置用户是否可以降级格式打印文档
            permissions.setCanPrintDegraded(outFileEncryptorEntity.getPrintDegraded());

            String strOwnerPwd = "";
            String strUserPwd = "";
            if(!StringUtils.isEmpty(outFileEncryptorEntity.getOwnerPassword())){
                strOwnerPwd = outFileEncryptorEntity.getOwnerPassword();
            }
            if(!StringUtils.isEmpty(outFileEncryptorEntity.getUserPassWord())){
                strUserPwd = outFileEncryptorEntity.getUserPassWord();
            }
            StandardProtectionPolicy standardProtectionPolicy = new StandardProtectionPolicy(
                    strOwnerPwd,
                    strUserPwd,
                    permissions
            );
            SecurityHandler securityHandler = new StandardSecurityHandler(standardProtectionPolicy);
            securityHandler.prepareDocumentForEncryption(pdDocument);
            PDEncryption pdEncryption = new PDEncryption();
            pdEncryption.setSecurityHandler(securityHandler);
            pdDocument.setEncryptionDictionary(pdEncryption);

            pdDocument.save(strPdfEnc);

            File filePdfEnc = new File(strPdfEnc);
            if(filePdfEnc.exists()){
                log.debug("加密后的PDF文件改名，文件{}到{}", strPdfEnc, strPdfFilePath);
                FileUtil.rename(filePdfEnc, strPdfFilePath, true);
            }

        }

        return filePdf;
    }

    /**
     * 获取PDF文件首页JPG图片
     * @param strInputPDF   输入的PDF文件路径和文件名
     * @param strOutputJpg  输出的JPG文件路径和文件名
     * @return 转换成功的JPG文件File对象
     */
    public File getFirstJpgFromPdf(String strInputPDF, String strOutputJpg) throws IOException {
        //将读取URL生成File
        File filePdf = new File(strInputPDF);

        // 打开来源 使用pdfbox中的方法
        PDDocument pdfDocument = Loader.loadPDF(filePdf);
        PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);

        // 以300 dpi 读取存入 BufferedImage 对象
        BufferedImage buffImage = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
        // 将 BufferedImage 写入到 png
        // ImageIOUtil.writeImage(buffImage, "c:/temp/xx.png", dpi);

        //文件储存对象
        File fileThumbnail = new File(strOutputJpg);
        // ImageIO.write(FrameToBufferedImage(frame), "jpg", outPut);
        ImageIO.write(buffImage, "jpg",fileThumbnail);

        // 关闭文档
        pdfDocument.close();

        //注意关闭文档与删除文档的顺序
        //删除临时的file
        FileUtil.del(filePdf);

        return fileThumbnail;

    }

    /**
     *  pdf文件转换成jpg图片集
     * @param pdfFile pdf文件
     * @return 图片访问集合
     */
    public List<String> pdf2jpg(File pdfFile) {
        List<String> imageUrls = new ArrayList<>();
        String imageFileSuffix = ".jpg";

        try {
            @Cleanup PDDocument doc = Loader.loadPDF(pdfFile);
            int pageCount = doc.getNumberOfPages();
            PDFRenderer pdfRenderer = new PDFRenderer(doc);

            String folder = ConvertConfig.outPutPath + DateUtil.today();
            String imageFilePath;
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                imageFilePath = folder + File.separator + pageIndex + imageFileSuffix;
                if (!FileUtil.exist(imageFilePath)){
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 105, ImageType.RGB);
                    ImgUtil.write(image, FileUtil.file(imageFilePath));
                }
                imageUrls.add(imageFilePath);
            }
        } catch (IOException e) {
            log.error("Convert pdf to jpg exception, pdfFilePath：{}", pdfFile.getPath(), e);
        }
        return imageUrls;
    }

}
