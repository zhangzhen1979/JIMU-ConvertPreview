package com.thinkdifferent.convertpreview.utils.convert4ofd;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.entity.OutFileEncryptorEntity;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CryptoException;
import org.ofd.render.OFDRender;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.ImageMaker;
import org.ofdrw.converter.SVGMaker;
import org.ofdrw.core.basicStructure.doc.permission.CT_Permission;
import org.ofdrw.core.basicStructure.doc.permission.Print;
import org.ofdrw.core.basicStructure.doc.permission.ValidPeriod;
import org.ofdrw.crypto.OFDEncryptor;
import org.ofdrw.crypto.enryptor.UserFEKEncryptor;
import org.ofdrw.crypto.enryptor.UserPasswordEncryptor;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.reader.OFDReader;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Order
@Component
@Log4j2
public class ConvertOfdUtil {

    public ConvertOfdUtil() {
        // 为不规范的字体名创建映射
        FontLoader.getInstance()
                .addAliasMapping("小标宋体", "方正小标宋简体")
                .addAliasMapping("KaiTi_GB2312", "楷体")
                .addAliasMapping("楷体", "KaiTi")

                .addSimilarFontReplaceRegexMapping(".*Kai.*", "楷体")
                .addSimilarFontReplaceRegexMapping(".*Kai.*", "楷体")
                .addSimilarFontReplaceRegexMapping(".*MinionPro.*", "SimSun")
                .addSimilarFontReplaceRegexMapping(".*SimSun.*", "SimSun")
                .addSimilarFontReplaceRegexMapping(".*Song.*", "宋体")
                .addSimilarFontReplaceRegexMapping(".*MinionPro.*", "SimSun");

        FontLoader.getInstance().scanFontDir(new File("src/main/resources/fonts"));
        FontLoader.setSimilarFontReplace(true);
    }

    /**
     * 将pdf文件转换为ofd文件。
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
     * @param strOfdFilePath     输入的ofd文件路径和文件名
     * @param strPdfFilePath     输出的pdf文件路径和文件名
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

    /**
     * 将ofd文件转换为jpg文件。
     *
     * @param strOfdFilePath     输入的ofd文件路径和文件名
     * @param strJpgFilePath     输出的jpg文件路径和文件名
     * @return 返回的JPG文件的List<String>对象
     */
    public List<String> convertOfd2Jpg(String strOfdFilePath, String strJpgFilePath) throws IOException {
        Path pathOfdIn = Paths.get(strOfdFilePath);
        log.debug("开始转换OFD->JPG文件{}到{}", strOfdFilePath, strJpgFilePath);

        List<String> listJpg = new ArrayList<>();

        try (OFDReader reader = new OFDReader(pathOfdIn);) {
            ImageMaker imageMaker = new ImageMaker(reader, 15);
            imageMaker.config.setDrawBoundary(false);
            for (int i = 0; i < imageMaker.pageSize(); i++) {
                BufferedImage image = imageMaker.makePage(i);
                Path dist = Paths.get(strJpgFilePath, i + ".jpg");
                ImageIO.write(image, "JPEG", dist.toFile());

                listJpg.add(strJpgFilePath + i + ".jpg");
            }
        }

        return listJpg;
    }

    /**
     * 将ofd文件转换为svg文件。
     *
     * @param strOfdFilePath     输入的ofd文件路径和文件名
     * @param strSvgFilePath     输出的svg文件路径和文件名
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
     * @param strOfdFilePath     输入的ofd文件路径和文件名
     * @param strHtmlFilePath    输出的html文件路径和文件名
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



}
