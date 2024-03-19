package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigPreview;
import com.thinkdifferent.convertpreview.entity.OutFileEncryptorEntity;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.utils.pdfUtil.ConvertPdfUtil;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CryptoException;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.ImageMaker;
import org.ofdrw.converter.SVGMaker;
import org.ofdrw.converter.ofdconverter.ImageConverter;
import org.ofdrw.converter.ofdconverter.PDFConverter;
import org.ofdrw.converter.ofdconverter.TextConverter;
import org.ofdrw.core.basicStructure.doc.permission.CT_Permission;
import org.ofdrw.core.basicStructure.doc.permission.Print;
import org.ofdrw.core.basicStructure.doc.permission.ValidPeriod;
import org.ofdrw.crypto.OFDEncryptor;
import org.ofdrw.crypto.enryptor.UserFEKEncryptor;
import org.ofdrw.crypto.enryptor.UserPasswordEncryptor;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.edit.Attachment;
import org.ofdrw.layout.element.Img;
import org.ofdrw.reader.OFDReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;

@Component
@Log4j2
public class ConvertOfdUtil {

    @Resource(name = "taskExecutor")
    private Executor executor;


    public ConvertOfdUtil() {
        // 为不规范的字体名创建映射
        FontLoader.getInstance()
                .addAliasMapping("小标宋体", "方正小标宋简体")
                .addAliasMapping("KaiTi_GB2312", "楷体")
                .addAliasMapping("楷体", "KaiTi")
                .addAliasMapping("宋体", "思源宋体")

                .addSimilarFontReplaceRegexMapping(".*Kai.*", "楷体")
                .addSimilarFontReplaceRegexMapping(".*Kai.*", "楷体")
                .addSimilarFontReplaceRegexMapping(".*MinionPro.*", "SimSun")
                .addSimilarFontReplaceRegexMapping(".*SimSun.*", "SimSun")
                .addSimilarFontReplaceRegexMapping(".*Song.*", "宋体")
                .addSimilarFontReplaceRegexMapping(".*SourceHanSerif*.*", "宋体")
                .addSimilarFontReplaceRegexMapping(".*MinionPro.*", "SimSun");

        // 读取默认字体
        FontLoader.getInstance().scanFontDir(new File(
                SystemUtil.beautifulPath(System.getProperty("user.dir")) + "fonts/"));

        if (cn.hutool.system.SystemUtil.getOsInfo().isWindows()) {
            // 读取Windows字体文件夹
            FontLoader.getInstance().scanFontDir(new File(
                    SystemUtil.beautifulPath(System.getenv("WINDIR")) + "Fonts/"));
            // 读取User文件夹下的字体文件夹
            FontLoader.getInstance().scanFontDir(new File(
                    SystemUtil.beautifulPath(System.getenv("USERPROFILE")) +
                            "AppData/Local/Microsoft/Windows/Fonts/"));
        } else if (cn.hutool.system.SystemUtil.getOsInfo().isLinux()) {
            FontLoader.getInstance().scanFontDir(new File("/usr/share/fonts/"));
        } else if (cn.hutool.system.SystemUtil.getOsInfo().isMacOsX()) {
            FontLoader.getInstance().scanFontDir(new File("/Library/Fonts/"));
            FontLoader.getInstance().scanFontDir(new File("/System/Library/Fonts/"));
            FontLoader.getInstance().scanFontDir(new File(
                    SystemUtil.beautifulPath(System.getProperty("user.home")) + "/Library/Fonts/"));
        } else if (cn.hutool.system.SystemUtil.getOsInfo().isHpUx()) {
            FontLoader.getInstance().scanFontDir(new File(
                    SystemUtil.beautifulPath(System.getProperty("user.home")) + "/.local/share/fonts/"));
            FontLoader.getInstance().scanFontDir(new File(
                    SystemUtil.beautifulPath(System.getProperty("user.home")) + "/.fonts/"));
            FontLoader.getInstance().scanFontDir(new File("/usr/share/fonts/"));
            FontLoader.getInstance().scanFontDir(new File("/usr/local/share/fonts/"));
        } else {
            FontLoader.getInstance().scanFontDir(new File("/usr/share/fonts/"));
        }
        FontLoader.setSimilarFontReplace(true);
    }

    /**
     * 将pdf文件转换为ofd文件。
     *
     * @param strPdfFilePath 输入的pdf文件路径和文件名
     * @param strOfdFilePath 输出的ofd文件路径和文件名
     * @return 返回的ofd文件的File对象
     */
    public File convertPdf2Ofd(String strPdfFilePath, String strOfdFilePath)
            throws IOException {
        Path pathPdfIn = Paths.get(strPdfFilePath);
        Path pathOfdOut = Paths.get(strOfdFilePath);
        log.debug("开始转换PDF->OFD文件{}到{}", strPdfFilePath, strOfdFilePath);
        try (PDFConverter converter = new PDFConverter(pathOfdOut)) {
            converter.convert(pathPdfIn);

            File filePdf = new File(strPdfFilePath);
            FileUtil.del(filePdf);
        }

        return new File(strOfdFilePath);
    }

    /**
     * 将txt件转换为ofd文件。
     *
     * @param strTxtFilePath 输入的pdf文件路径和文件名
     * @param strOfdFilePath 输出的ofd文件路径和文件名
     * @return 返回的ofd文件的File对象
     */
    public File convertTxt2Ofd(String strTxtFilePath, String strOfdFilePath)
            throws IOException {
        Path pathTxtIn = Paths.get(strTxtFilePath);
        Path pathOfdOut = Paths.get(strOfdFilePath);
        log.debug("开始转换TXT->OFD文件{}到{}", strTxtFilePath, strOfdFilePath);
        try (TextConverter converter = new TextConverter(pathOfdOut)) {
            converter.convert(pathTxtIn);
        }

        return new File(strOfdFilePath);
    }

    /**
     * 将图片文件转换为ofd文件。支持PNG、JPG、BPM，支持多文件处理
     *
     * @param strImgFilePath 输入的pdf文件路径和文件名, 多个图片文件为其父级目录
     * @param strOfdFilePath 输出的ofd文件路径和文件名
     * @return 返回的ofd文件的File对象
     */
    public File convertImg2Ofd(String strImgFilePath, String strOfdFilePath)
            throws IOException {
        log.debug("开始转换IMG->OFD文件{}到{}", strImgFilePath, strOfdFilePath);

        Path pathOfdOut = Paths.get(strOfdFilePath);

        try (ImageConverter converter = new ImageConverter(pathOfdOut)) {
            File fileImageFilePath = new File(strImgFilePath);
            if (FileUtil.isFile(fileImageFilePath)) {
                getConverter(converter, fileImageFilePath);
            } else if (fileImageFilePath.isDirectory() && fileImageFilePath.listFiles().length == 1) {
                getConverter(converter, fileImageFilePath.listFiles()[0]);
            } else {
                // 多文件需循环处理
                File[] imgFileNames = Objects.requireNonNull(fileImageFilePath.listFiles());
                // 计算每个图片缩放的倍数
                for (File imageFile : imgFileNames) {
                    getConverter(converter, imageFile);
                }
            }
        }

        return new File(strOfdFilePath);
    }


    private void getConverter(ImageConverter converter, File imageFile) throws IOException {
        BufferedImage image = Img.readImage(imageFile);
        double imgWidth = image.getWidth();
        double imgHeight = image.getHeight();
        PageLayout pageLayout = new PageLayout(imgWidth, imgHeight);
        converter.setPageSize(pageLayout);
        converter.append(imageFile.toPath(), pageLayout.getWidth(), pageLayout.getHeight());
    }

    /**
     * 对OFD文件加密
     *
     * @param strOfdFilePath         输出的ofd文件路径和文件名
     * @param outFileEncryptorEntity 输出文件加密对象
     * @return 返回的ofd文件的File对象
     */
    public File ofdEncry(String strOfdFilePath, OutFileEncryptorEntity outFileEncryptorEntity)
            throws IOException, CryptoException, GeneralSecurityException, ParseException {
        File fileOfd = new File(strOfdFilePath);
        File fileOfdEnc = new File(strOfdFilePath + "_enc.ofd");

        if(outFileEncryptorEntity != null && outFileEncryptorEntity.getEncry()){
            Path pathSource = Paths.get(strOfdFilePath);
            Path pathTarget = Paths.get(strOfdFilePath + "_enc.ofd");
            log.debug("开始对OFD文件加密码，文件{}到{}", strOfdFilePath, strOfdFilePath + "_enc.ofd");

            try (OFDReader reader = new OFDReader(pathSource);
                 OFDDoc ofdDoc = new OFDDoc(reader, pathTarget)){
                org.ofdrw.core.basicStructure.doc.Document doc = ofdDoc.getOfdDocument();

                CT_Permission permission = new CT_Permission();
                // 签章操作权限
                permission.setSignature(outFileEncryptorEntity.getSignature());
                // 水印操作权限
                permission.setWatermark(outFileEncryptorEntity.getWatermark());
                // 打印操作权限
                permission.setPrint(new Print(outFileEncryptorEntity.getPrint(), outFileEncryptorEntity.getCopies()));
                permission.setPrintScreen(outFileEncryptorEntity.getPrint());
                // 导出操作权限
                permission.setExport(outFileEncryptorEntity.getExport());
                // 批注操作权限
                permission.setAnnot(outFileEncryptorEntity.getModifyAnnotations());
                // 编辑操作权限（文档信息权限、元数据权限、公文元数据权限）
                permission.setEdit(outFileEncryptorEntity.getModify());
                // 有效期
                if(!StringUtils.isEmpty(outFileEncryptorEntity.getValidPeriodStart())
                        && !StringUtils.isEmpty(outFileEncryptorEntity.getValidPeriodEnd())){
                    SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd");
                    Date dateStart = sdfInput.parse(outFileEncryptorEntity.getValidPeriodStart());
                    Date dateEnd = sdfInput.parse(outFileEncryptorEntity.getValidPeriodEnd());

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    LocalDateTime ldtStart = LocalDateTime.parse(sdf.format(dateStart));
                    LocalDateTime ldtEnd = LocalDateTime.parse(sdf.format(dateEnd));
                    permission.setValidPeriod(
                            new ValidPeriod(
                                    ldtStart,
                                    ldtEnd
                            )
                    );

                }

                doc.setPermissions(permission);
            }

            FileUtil.rename(pathTarget, strOfdFilePath, true);

            if(!StringUtils.isEmpty(outFileEncryptorEntity.getUserPassWord())){
                try (OFDEncryptor ofdEncryptor = new OFDEncryptor(pathSource, pathTarget)) {
                    String strUserName = "admin";
                    if(!StringUtils.isEmpty(outFileEncryptorEntity.getUserName())){
                        strUserName = outFileEncryptorEntity.getUserName();
                    }

                    final UserFEKEncryptor encryptor = new UserPasswordEncryptor(
                            // 目前，加密后的OFD建议使用“超越版式办公套件”（不要用数科OFD阅读器，无法打开加密后的OFD）
                            // 此工具个人版仅支持用户名为admin的加密信息。
                            strUserName,
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
     * 将ofd文件转换为pdf文件。
     *
     * @param strOfdFilePath 输入的ofd文件路径和文件名
     * @param strPdfFilePath 输出的pdf文件路径和文件名
     * @return 返回的pdf文件的File对象
     */
    public File convertOfd2Pdf(String strOfdFilePath, String strPdfFilePath) {
        Path pathOfdIn = Paths.get(strOfdFilePath);
        Path pathPdfOut = Paths.get(strPdfFilePath);
        log.debug("开始转换OFD->PDF文件{}到{}", strOfdFilePath, strPdfFilePath);
        // OFD转换PDF
        ConvertHelper.toPdf(pathOfdIn, pathPdfOut);

        return new File(strPdfFilePath);
    }

    @Autowired(required = false)
    private ConvertPdfUtil convertPdfUtil;

    /**
     * 将ofd文件转换为jpg文件。
     *
     * @param fileOfd   输入的ofd文件路径和文件名
     * @param listPages 截取页码
     * @return 返回的JPG文件的List<String>对象
     */
    public List<String> convertOfd2Jpg(File fileOfd, List<Integer> listPages, boolean blnAsync) throws IOException {
        return convertOfd2Jpg(fileOfd, listPages,
                SystemUtil.beautifulPath(ConvertDocConfigBase.outPutPath) + FileUtil.mainName(fileOfd), blnAsync);
    }

    /**
     * 将ofd文件转换为jpg文件。
     *
     * @param fileOfd     输入的ofd文件路径和文件名
     * @param listPages   截取页码
     * @param jpgFilePath jpg存储的输出路径（临时文件）
     * @param blnAsync    是否开启异步转换
     * @return 返回的JPG文件的List<String>对象
     */
    public List<String> convertOfd2Jpg(File fileOfd, List<Integer> listPages, String jpgFilePath, boolean blnAsync) throws IOException {
        if (FileUtil.extName(fileOfd).equals("jpg")) {
            return Collections.singletonList(FileUtil.getCanonicalPath(fileOfd));
        }
        log.debug("开始转换OFD->JPG文件{}", fileOfd);

        Path pathOfd = Paths.get(fileOfd.getAbsolutePath());
        List<String> imageUrls = new ArrayList<>();

        // OFD总页数
        int intBasePageNum = 0;
        // 已转换完成的jpg数量（页数）
        int finishedImgNum = 0;
        // 将ofd转换为多个jpg文件
        try (OFDReader reader = new OFDReader(pathOfd)) {
            intBasePageNum = reader.getNumberOfPages();
            int pageCount = reader.getNumberOfPages();
            // 异步转换的图片数量
            if (blnAsync && ConvertDocConfigPreview.asyncImgNum > 0) {
                pageCount = Math.min(ConvertDocConfigPreview.asyncImgNum, pageCount);
            }

            File fileFolder = new File(jpgFilePath);
            // 如果是文件，则删除（后面要创建同名文件夹）
            if (fileFolder.isFile()) {
                fileFolder.delete();
            }

            if (!fileFolder.exists()) {
                // 不存在创建目录， 转换
                fileFolder.mkdirs();
            } else {
                // 已存在，不删除，如果已转化的图片数量 < 首次转换的数量，进行转换，其他，直接返回现有图片数量
                finishedImgNum = FileUtil.listFileNames(jpgFilePath).size();
                if (ConvertDocConfigPreview.asyncImgNum > 0) {
                    pageCount = Math.max(pageCount, finishedImgNum);
                }
            }

            // 转换为图片
            ImageMaker imageMaker = new ImageMaker(reader, 15);
            imageMaker.config.setDrawBoundary(false);

            String strJpgPath = jpgFilePath + "/";
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                if (CollectionUtil.isNotEmpty(listPages) && !listPages.contains(pageIndex + 1)) {
                    // 如果设置了指定页转换，并且当前页不在控制列表中，则跳过，不转换。
                    continue;
                }

                BufferedImage image = imageMaker.makePage(pageIndex);
                Path dist = Paths.get(strJpgPath, pageIndex + ".jpg");
                ImageIO.write(image, "JPG", dist.toFile());

                String strOutputJpg = strJpgPath + pageIndex + ".jpg";
                imageUrls.add(strOutputJpg);
            }
        } catch (IOException e) {
            log.error("Convert OFD to JPG Exception, ofdFilePath：{}", fileOfd.getPath(), e);
        }

        // 启用异步转换，另起线程继续进行文件转换
        if (ConvertDocConfigPreview.asyncImgNum > 0 && intBasePageNum > ConvertDocConfigPreview.asyncImgNum &&
                intBasePageNum > finishedImgNum) {
            executor.execute(() -> {
                try (OFDReader reader = new OFDReader(pathOfd)) {
                    int asyncPageCount = reader.getNumberOfPages();

                    ImageMaker imageMaker = new ImageMaker(reader, 15);
                    imageMaker.config.setDrawBoundary(false);

                    String strAsyncJpgPath = jpgFilePath + "/";
                    for (int pageIndex = imageUrls.size(); pageIndex < asyncPageCount; pageIndex++) {
                        if (CollectionUtil.isNotEmpty(listPages) && !listPages.contains(pageIndex + 1)) {
                            // 如果设置了指定页转换，并且当前页不在控制列表中，则跳过，不转换。
                            continue;
                        }
                        BufferedImage image = imageMaker.makePage(pageIndex);
                        Path dist = Paths.get(strAsyncJpgPath, pageIndex + ".jpg");
                        ImageIO.write(image, "JPG", dist.toFile());
                    }
                } catch (IOException e) {
                    log.error("Async Convert pdf to jpg exception, pdfFilePath：{}", fileOfd.getPath(), e);
                }
            });
        }
        return imageUrls;
    }

    /**
     * 将ofd文件转换为svg文件。
     *
     * @param strOfdFilePath 输入的ofd文件路径和文件名
     * @param strSvgFilePath 输出的svg文件路径和文件名
     * @return 返回的SVG文件的List<String>对象
     */
    public List<String> convertOfd2Svg(String strOfdFilePath, String strSvgFilePath) throws IOException {
        Path pathOfdIn = Paths.get(strOfdFilePath);
        log.debug("开始转换OFD->SVG文件{}到{}", strOfdFilePath, strSvgFilePath);

        List<String> listSvg = new ArrayList<>();

        try (OFDReader reader = new OFDReader(pathOfdIn);) {
            SVGMaker svgMaker = new SVGMaker(reader, 5d);
            svgMaker.config.setDrawBoundary(false);
            svgMaker.config.setClip(false);
            for (int i = 0; i < svgMaker.pageSize(); i++) {
                String svg = svgMaker.makePage(i);
                Path dist = Paths.get(strSvgFilePath, i + ".svg");
                Files.write(dist, svg.getBytes());

                listSvg.add(strSvgFilePath + i + ".jpg");
            }
        }

        return listSvg;
    }


    /**
     * 将ofd文件转换为html文件。
     *
     * @param strOfdFilePath  输入的ofd文件路径和文件名
     * @param strHtmlFilePath 输出的html文件路径和文件名
     * @return 返回的html文件的File对象
     */
    public File convertOfd2Html(String strOfdFilePath, String strHtmlFilePath) throws IOException {
        // 1. 提供文档
        Path pathOfdIn = Paths.get(strOfdFilePath);
        Path pathHtmlOut = Paths.get(strHtmlFilePath);
        log.debug("开始转换OFD->HTML文件{}到{}", strOfdFilePath, strHtmlFilePath);
        // 2. [可选]配置字体，别名，扫描目录等
        // FontLoader.getInstance().addAliasMapping(null, "小标宋体", "方正小标宋简体", "方正小标宋简体")
        // FontLoader.getInstance().scanFontDir(new File("src/test/resources/fonts"));
        // 3. 配置参数（HTML页面宽度(px)），转换并存储HTML到文件。
        ConvertHelper.toHtml(pathOfdIn, pathHtmlOut, 1000);

        return new File(strHtmlFilePath);
    }

    /**
     * 将传入的附件加入到OFD中
     *
     * @param fileInput  输入文件（无附件）
     * @param listInputs 附件数组
     * @return
     */
    @SneakyThrows
    public File addAttachments(File fileInput, List<Input> listInputs, String strInputType) {
        Path pathSource = Paths.get(fileInput.getAbsolutePath());
        Path pathTarget = Paths.get(fileInput.getAbsolutePath() + ".ofd");

        try (OFDReader reader = new OFDReader(pathSource);
             OFDDoc ofdDoc = new OFDDoc(reader, pathTarget)) {
            // 加入附件文件
            for (int i = 0; i < listInputs.size(); i++) {
                String strFileName = listInputs.get(i).getInputFile().getAbsolutePath();
                strFileName = SystemUtil.beautifulFilePath(strFileName);
                Path pathAtt = Paths.get(strFileName);

                String strDispName = strFileName.substring(strFileName.lastIndexOf("/") + 1);
                ofdDoc.addAttachment(new Attachment(strDispName, pathAtt));

                if (!StringUtils.equalsAnyIgnoreCase(strInputType, "path", "local")) {
                    FileUtil.del(strFileName);
                    if (FileUtil.isDirEmpty(pathAtt.getParent())) {
                        FileUtil.del(pathAtt.getParent());
                    }
                }
            }

        }

        FileUtil.rename(pathTarget.toFile(), fileInput.getAbsolutePath(), true);

        return fileInput;
    }

}
