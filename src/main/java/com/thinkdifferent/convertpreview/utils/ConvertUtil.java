package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import com.lowagie.text.pdf.codec.TiffImage;
import com.sun.media.jai.codec.*;
import com.thinkdifferent.convertpreview.cache.CacheManager;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import com.thinkdifferent.convertpreview.entity.OutFileEncryptorEntity;
import com.thinkdifferent.convertpreview.entity.Thumbnail;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.bouncycastle.crypto.CryptoException;
import org.jodconverter.DocumentConverter;
import org.ofd.render.OFDRender;
import org.ofdrw.crypto.OFDEncryptor;
import org.ofdrw.crypto.enryptor.UserFEKEncryptor;
import org.ofdrw.crypto.enryptor.UserPasswordEncryptor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Order
@Component
@Log4j2
public class ConvertUtil {

    /**
     * 图片 转  JPG。
     * 支持输入格式如下：BMP、GIF、FlashPix、JPEG、PNG、PMN、TIFF、WBMP
     *
     * @param strInputFile  输入文件的路径和文件名
     * @param strOutputFile 输出文件的路径和文件名
     * @return
     */
    public List<String> convertPic2Jpg(String strInputFile, String strOutputFile)
            throws IOException {
        List<String> listImageFiles = new ArrayList<>();
        Assert.isTrue(StringUtils.isNotBlank(strInputFile), "文件名为空");
        File inputFile = new File(strInputFile);
        Assert.isTrue(inputFile.exists(), "找不到文件【" + strInputFile + "】");
        strInputFile = SystemUtil.beautifulFilePath(strInputFile);
        strOutputFile = SystemUtil.beautifulFilePath(strOutputFile);

        try {
            // create file
            FileUtil.touch(strOutputFile);
            BufferedImage image = ImageIO.read(inputFile);
            int width = image.getWidth();
            int height = image.getHeight();

            Thumbnails.of(inputFile)
                    .size(width, height)
                    .outputFormat("jpg")
                    .toFile(strOutputFile);
            image = null;
            // inputFile.delete();
            return Collections.singletonList(strOutputFile);
        } catch (Exception e) {
            log.error("转换单页jpg出现问题，使用旧方法", e);
        }

        // 老办法，解决Thumbnails组件部分格式不兼容的问题。
        String strFilePrefix = strOutputFile.substring(strOutputFile.lastIndexOf("/") + 1, strOutputFile.lastIndexOf("."));
        String strFileExt = strInputFile.substring(strInputFile.lastIndexOf(".") + 1).toUpperCase();

        @Cleanup FileSeekableStream fileSeekStream = new FileSeekableStream(strInputFile);

        ImageDecoder imageDecoder = ImageCodec.createImageDecoder(getPicType(strFileExt), fileSeekStream, null);
        int intPicCount = imageDecoder.getNumPages();
        log.info("该" + strFileExt + "文件共有【" + intPicCount + "】页");

        String strJpgPath = "";
        if (intPicCount == 1) {
            // 如果是单页tif文件，则转换的目标文件夹就在指定的位置
            strJpgPath = strOutputFile.substring(0, strOutputFile.lastIndexOf("/"));
        } else {
            // 如果是多页tif文件，则在目标文件夹下，按照文件名再创建子目录，将转换后的文件放入此新建的子目录中
            strJpgPath = strOutputFile.substring(0, strOutputFile.lastIndexOf("."));
        }

        // 处理目标文件夹，如果不存在则自动创建
        File fileJpgPath = new File(strJpgPath);
        if (!fileJpgPath.exists()) {
            fileJpgPath.mkdirs();
        }

        PlanarImage in = JAI.create("stream", fileSeekStream);
        // OutputStream os = null;
        JPEGEncodeParam param = new JPEGEncodeParam();

        // 循环，处理每页tif文件，转换为jpg
        for (int i = 0; i < intPicCount; i++) {
            String strJpg;
            if (intPicCount == 1) {
                strJpg = strJpgPath + "/" + strFilePrefix + ".jpg";
            } else {
                strJpg = strJpgPath + "/" + i + ".jpg";
            }

            File fileJpg = new File(strJpg);
            @Cleanup OutputStream os = new FileOutputStream(strJpg);
            ImageEncoder enc = ImageCodec.createImageEncoder("JPEG", os, param);
            enc.encode(in);

            log.info("每页分别保存至： " + fileJpg.getCanonicalPath());

            listImageFiles.add(strJpg);
        }

        return listImageFiles;
    }

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
            List<String> listPic2Jpg = convertPic2Jpg(strTifFile, strJpg);

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

    private String getPicType(String strExt) {
        switch (strExt.toUpperCase()) {
            case "JPG":
                return "JPEG";
            case "TIF":
                return "TIFF";
            default:
                return strExt.toUpperCase();
        }
    }

    /**
     * 将pdf文件转换为ofd文件。可对OFD文件设置密码
     *
     * @param strPdfFilePath     输入的pdf文件路径和文件名
     * @param strOfdFilePath     输出的ofd文件路径和文件名
     * @return 返回的ofd文件的File对象
     */
    public File convertPdf2Ofd(String strPdfFilePath, String strOfdFilePath)
            throws IOException {
        Path pathPdfIn = Paths.get(strPdfFilePath);
        Path pathOfdOut = Paths.get(strOfdFilePath);
        log.debug("开始转换PDF->OFD文件{}到{}", strPdfFilePath, strOfdFilePath);
        @Cleanup InputStream inputStream = Files.newInputStream(pathPdfIn);
        @Cleanup OutputStream otherStream = Files.newOutputStream(pathOfdOut);
        OFDRender.convertPdfToOfd(inputStream, otherStream);

        return new File(strOfdFilePath);
    }


    /**
     * 对OFD文件加密
     *
     * @param strOfdFilePath         输出的ofd文件路径和文件名
     * @param outFileEncryptorEntity 输出文件加密对象
     * @return 返回的ofd文件的File对象
     */
    public File ofdEncry(String strOfdFilePath, OutFileEncryptorEntity outFileEncryptorEntity)
            throws IOException, CryptoException, GeneralSecurityException {
        File fileOfd = new File(strOfdFilePath);
        File fileOfdEnc = new File(strOfdFilePath + "_enc.ofd");

        if(outFileEncryptorEntity != null && outFileEncryptorEntity.getEncry()){
            Path pathSource = Paths.get(strOfdFilePath);
            Path pathTarget = Paths.get(strOfdFilePath + "_enc.ofd");
            log.debug("开始对OFD文件加密码，文件{}到{}", strOfdFilePath, strOfdFilePath + "_enc.ofd");

            if(!StringUtils.isEmpty(outFileEncryptorEntity.getUserName()) && !StringUtils.isEmpty(outFileEncryptorEntity.getUserPassWord())){

                try (OFDEncryptor ofdEncryptor = new OFDEncryptor(pathSource, pathTarget)) {
                    final UserFEKEncryptor encryptor = new UserPasswordEncryptor(
                            outFileEncryptorEntity.getUserName(),
                            outFileEncryptorEntity.getUserPassWord()
                    );
                    ofdEncryptor.addUser(encryptor);
                    ofdEncryptor.encrypt();
                    log.debug("Encryptor OFD: " + pathTarget.toAbsolutePath().toString());

                    if(fileOfdEnc.exists()){
                        log.debug("加密后的OFD文件改名，文件{}到{}", strOfdFilePath + "_enc.ofd", strOfdFilePath);
                        ofdEncryptor.close();
                        FileUtil.rename(fileOfdEnc, strOfdFilePath, true);
                    }
                }

             }

        }

        return fileOfd;
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
            permissions.setCanAssembleDocument(false);
            // 是否可以复制和提取内容
            permissions.setCanExtractContent(outFileEncryptorEntity.getCopy());

            permissions.setCanExtractForAccessibility(false);
            // 设置用户是否可以填写交互式表单字段（包括签名字段）
            permissions.setCanFillInForm(false);
            // 设置用户是否可以修改文档
            permissions.setCanModify(outFileEncryptorEntity.getModify());
            // 设置用户是否可以添加或修改文本注释并填写交互式表单字段，如果canModify()返回true，则创建或修改交互式表单字段（包括签名字段）。
            permissions.setCanModifyAnnotations(false);
            // 设置用户是否可以打印。
            permissions.setCanPrint(outFileEncryptorEntity.getPrint());
            // 设置用户是否可以降级格式打印文档
            permissions.setCanPrintDegraded(false);

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
     * 将传入的jpg文件转换为pdf、ofd文件
     *
     * @param inputFilePath 传入的文件路径
     * @param inputFileType 传入的文件类型
     * @param listJpg       转换后的jpg图片
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

    /*************************** 图片处理相关方法 **************************/

    /**
     * 使用给定的图片生成指定大小的图片（原格式）
     *
     * @param strInputFilePath  输入文件的绝对路径和文件名
     * @param strOutputFilePath 输出文件的绝对路径和文件名
     * @param intWidth          输出文件的宽度
     * @param intHeight         输出文件的高度
     */
    public File fixedSizeImage(String strInputFilePath, String strOutputFilePath,
                                      int intWidth, int intHeight)
            throws IOException {
        Thumbnails.of(strInputFilePath).
                size(intWidth, intHeight).
                toFile(strOutputFilePath);
        return new File(strOutputFilePath);
    }

    /**
     * 按比例缩放图片
     *
     * @param strInputFilePath  输入文件的绝对路径和文件名
     * @param strOutputFilePath 输出文件的绝对路径和文件名
     * @param dblScale          输出文件的缩放百分比。1为100%,0.8为80%，以此类推。
     * @param dblQuality        输出文件的压缩比（质量）。1为100%,0.8为80%，以此类推。
     */
    public File thumbnail(String strInputFilePath, String strOutputFilePath,
                                 double dblScale, double dblQuality)
            throws IOException {
        Thumbnails.of(strInputFilePath).
                //scalingMode(ScalingMode.BICUBIC).
                        scale(dblScale). // 图片缩放80%, 不能和size()一起使用
                outputQuality(dblQuality). // 图片质量压缩80%
                toFile(strOutputFilePath);
        return new File(strOutputFilePath);
    }

    public File getThumbnail(ConvertEntity convertEntity, List<String> listJpg)
            throws IOException {
        File fileOut = null;
        Thumbnail thumbnail = convertEntity.getThumbnail();
        if (thumbnail.getWidth() > 0 || thumbnail.getHeight() > 0) {
            // 如果输入了边长，则按边长生成
            int intImageWidth = 0;
            int intImageHeight = 0;
            try (FileInputStream fis = new FileInputStream(new File(listJpg.get(0)))) {
                BufferedImage buffSourceImg = ImageIO.read(fis);
                BufferedImage buffImg = new BufferedImage(buffSourceImg.getWidth(), buffSourceImg.getHeight(), BufferedImage.TYPE_INT_RGB);
                // 获取图片的大小
                intImageWidth = buffImg.getWidth();
                intImageHeight = buffImg.getHeight();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (thumbnail.getWidth() > 0 && thumbnail.getHeight() == 0) {
                // 如果只输入了宽，则按比例计算高
                thumbnail.setHeight(thumbnail.getWidth() * intImageHeight / intImageWidth);
            } else if (thumbnail.getWidth() == 0 && thumbnail.getHeight() > 0) {
                // 如果只输入了高，则按比例计算宽
                thumbnail.setWidth(thumbnail.getHeight() * intImageWidth / intImageHeight);
            }

            fileOut = fixedSizeImage(
                    listJpg.get(0),
                    convertEntity.getWriteBack().getOutputPath() + convertEntity.getOutPutFileName() + ".jpg",
                    thumbnail.getWidth(),
                    thumbnail.getHeight()
            );

        } else if (thumbnail.getScale() >= 0d) {
            // 如果输入了比例，则按比例生成
            fileOut = thumbnail(
                    listJpg.get(0),
                    convertEntity.getWriteBack().getOutputPath() + convertEntity.getOutPutFileName() + ".jpg",
                    thumbnail.getScale(),
                    thumbnail.getQuality()
            );

        }

        return fileOut;
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
