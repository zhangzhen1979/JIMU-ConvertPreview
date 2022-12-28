package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import com.lowagie.text.pdf.codec.TiffImage;
import com.thinkdifferent.convertpreview.cache.CacheManager;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import com.thinkdifferent.convertpreview.entity.OutFileEncryptorEntity;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jodconverter.DocumentConverter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
     * 将Jpg图片转换为Pdf文件
     *
     * @param listJpgFile 输入的jpg的路径和文件名的List对象
     * @param strPdfFile  输出的pdf的路径和文件名
     * @return PDF文件的File对象
     */
    public File convertJpg2Pdf(List<String> listJpgFile, String strPdfFile) {
        log.info("jpg {} 转pdf {}", String.join(" ", listJpgFile), strPdfFile);
        Document document = null;
        try {
            document = new Document();

        // 设置文档页边距
        document.setMargins(0, 0, 0, 0);
        FileOutputStream fos = new FileOutputStream(strPdfFile);
            PdfWriter.getInstance(document, fos);
            // 打开文档
            document.open();

            // 循环，读取每个文件，添加到pdf的document中。
            for (String strJpgFile : listJpgFile) {
                // 获取图片的宽高
                Image image = Image.getInstance(strJpgFile);
                float floatImageHeight = image.getScaledHeight();
                float floatImageWidth = image.getScaledWidth();
                // 设置页面宽高与图片一致
                Rectangle rectangle = new Rectangle(floatImageWidth, floatImageHeight);
                document.setPageSize(rectangle);
                // 图片居中
                image.setAlignment(Element.ALIGN_CENTER);
                //新建一页添加图片
                document.newPage();
                document.add(image);
            }

            fos.flush();

        }catch (Exception e){
            log.error(e);
        }finally {
            if (document != null && document.isOpen()){
                document.close();
            }
        }
        return new File(strPdfFile);
    }

    /**
     * 将Tif图片转换为Pdf文件（支持多页Tif）
     *
     * @param strTifFile 输入的tif的路径和文件名
     * @param strPdfFile 输出的pdf的路径和文件名
     * @return
     */
    public File convertTif2Pdf(String strTifFile, String strPdfFile)
            throws IOException, DocumentException {
        @Cleanup RandomAccessFileOrArray rafa = new RandomAccessFileOrArray(strTifFile);
        @Cleanup Document document = new Document();
        // 设置文档页边距
        document.setMargins(0, 0, 0, 0);

        PdfWriter.getInstance(document, new FileOutputStream(strPdfFile));
        document.open();
        int intPages = TiffImage.getNumberOfPages(rafa);

        if (intPages == 1) {
            String strJpg = strTifFile.substring(0, strTifFile.lastIndexOf(".")) + ".jpg";
            File fileJpg = new File(strJpg);
            ConvertJpgUtil convertJpgUtil = new ConvertJpgUtil();
            List<String> listPic2Jpg = convertJpgUtil.convertPic2Jpg(strTifFile, strJpg);

            if (fileJpg.exists()) {
                if (FileUtil.exist(convertJpg2Pdf(listPic2Jpg, strPdfFile))) {
                    FileUtil.del(fileJpg);
                }
            }
        } else {
            Image image;
            for (int i = 1; i <= intPages; i++) {
                image = TiffImage.getTiffImage(rafa, i);
                // 设置页面宽高与图片一致
                Rectangle pageSize = new Rectangle(image.getScaledWidth(), image.getScaledHeight());
                document.setPageSize(pageSize);
                // 图片居中
                image.setAlignment(Element.ALIGN_CENTER);
                //新建一页添加图片
                document.newPage();
                document.add(image);
            }
        }

        return new File(strPdfFile);
    }



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
     * 将传入的jpg文件转换为pdf文件
     *
     * @param inputFilePath 传入的文件路径
     * @param inputFileType 传入的文件类型
     * @param listJpg       传入的jpg图片
     * @param convertEntity 配置信息
     */
    public File convertPic2Pdf(String inputFilePath, String inputFileType,
                                      List<String> listJpg, ConvertEntity convertEntity)
            throws IOException, DocumentException {
        // 文件输出格式
        String outPutFileType = convertEntity.getOutPutFileType();
        File fileReturn = null;
        if ("pdf".equalsIgnoreCase(outPutFileType) || "ofd".equalsIgnoreCase(outPutFileType)) {
            // 将传入的jpg文件转换为pdf、ofd文件，存放到输出路径中
            String strPdfFilePath = convertEntity.getWriteBack().getOutputPath() + convertEntity.getOutPutFileName() + ".pdf";
            if ("tif".equalsIgnoreCase(inputFileType)) {
                if (Objects.isNull(convertEntity.getPngMark()) &&
                        Objects.isNull(convertEntity.getTextMark()) &&
                        Objects.isNull(convertEntity.getFirstPageMark())) {
                    // 如果没有水印 且 没有归档章，直接用tif转pdf
                    fileReturn = convertTif2Pdf(inputFilePath, strPdfFilePath);
                } else {
                    // 有水印 或 有归档章
                    fileReturn = convertJpg2Pdf(listJpg, strPdfFilePath);
                }
            } else {
                // 非 tif文件
                fileReturn = convertJpg2Pdf(listJpg, strPdfFilePath);
            }

            // 双层PDF
            if (!CollectionUtils.isEmpty(convertEntity.getContexts())) {
                // 双层PDF修改
                DoubleLayerPdfUtil.addText(fileReturn, convertEntity.getContexts());
            }

        }
        return fileReturn;
    }


    /*************************** Office文件处理相关方法 **************************/
    @Resource
    private CacheManager cacheManager;

    /**
     * 将输入文件转换为pdf
     *
     * @param strInputFile  输入的文件路径和文件名
     * @param strOutputFile 输出的文件路径和文件名
     * @param convertEntity 转换参数
     * @return file
     */
    @SneakyThrows
    public File convertOffice2Pdf(String strInputFile, String strOutputFile,
                                  ConvertEntity convertEntity) {
        File fileInput = new File(strInputFile);
        Assert.isTrue(fileInput.exists(), "输入文件【" + strInputFile + "】不存在");

        // 如果输出文件名不是pdf扩展名，加上
        if (!strOutputFile.endsWith(".pdf")) {
            strOutputFile = strOutputFile + ".pdf";
        }
        File fileOutput = new File(strOutputFile);

        // 如果输入的文件存在，则执行转换
        // 如果配置了本地应用程序工具，则执行本地程序；
        if(ConvertConfig.wpsEnabled || ConvertConfig.officeEnabled){
            // 开始时间
            long stime = System.currentTimeMillis();

            boolean blnFlag = LocalConvertUtil.process(strInputFile, strOutputFile);

            // 结束时间
            long etime = System.currentTimeMillis();
            // 计算执行时间
            if(blnFlag){
                log.info("Convert PDF success, Use time is: " + (int)((etime - stime)/1000) + " s...");
            }else{
                log.info("Convert PDF fail, Use time is: " + (int)((etime - stime)/1000) + " s...");
                return null;
            }

        }else if(ConvertConfig.libreOfficeEnabled){
            // 如果没有，则调用LibreOffice转换。
            getDocumentConverter().convert(fileInput).to(fileOutput).execute();
        }

        return fileOutput;
    }

    private DocumentConverter getDocumentConverter() {
        return SpringUtil.getClass(DocumentConverter.class);
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
