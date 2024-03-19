package com.thinkdifferent.convertpreview.utils.watermark;

import com.thinkdifferent.convertpreview.entity.ConvertDocEntity;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class PdfWaterMarkUtil {

    /**
     * 给PDF添加水印
     *
     * @param strSourcePdf     源PDF
     * @param strTargetPdf     目标PDF
     * @param convertDocEntity 转换参数
     * @param intPageNum       上一个文件的最后页码
     * @param intPageCount     截止到上一个文件的最后总页数
     */
    public static void mark4Pdf(String strSourcePdf,
                                String strTargetPdf,
                                ConvertDocEntity convertDocEntity,
                                int intPageNum,
                                int intPageCount) throws Exception {
        File fileInputPdf = new File(strSourcePdf);
        strTargetPdf = SystemUtil.beautifulFilePath(strTargetPdf);

        //打开pdf文件
        @Cleanup PDDocument pdDocument = Loader.loadPDF(fileInputPdf);
        pdDocument.setAllSecurityToBeRemoved(true);

        PDExtendedGraphicsState pdExtGfxState = new PDExtendedGraphicsState();
        pdExtGfxState.setAlphaSourceFlag(true);
        pdExtGfxState.getCOSObject().setItem(COSName.BM, COSName.MULTIPLY);

        for (int i = 0; i < pdDocument.getNumberOfPages(); i++) {
            PDPage page = pdDocument.getPage(i);
            @Cleanup PDPageContentStream contentStream = new PDPageContentStream(pdDocument, page,
                    PDPageContentStream.AppendMode.APPEND, true, true);
            // 水印位置修正
            float modifyX = 0f;

            // 如果添加图片水印，则进行如下处理
            if (convertDocEntity.getPngMark() != null) {
                convertDocEntity.getPngMark().mark4Pdf(pdExtGfxState,
                        contentStream,
                        pdDocument,
                        page,
                        convertDocEntity.getPngMark(),
                        null,
                        convertDocEntity.getAlpha());

                modifyX = convertDocEntity.getPngMark().getLocateX() == 0f ? 150F : convertDocEntity.getPngMark().getLocateX();
            }

            // 如果添加页码，则进行如下处理
            if (convertDocEntity.getPageNum() != null &&
                    convertDocEntity.getPageNum().isEnable()) {
                // 如果设置为单个文件single，且输入文件也是单个，则加页码
                if (("single".equalsIgnoreCase(convertDocEntity.getPageNum().getType()) &&
                        convertDocEntity.getInputFiles().size() == 1) ||
                        // 如果设置为多个文件mutli，且输入文件也是多个，则加页码
                        ("multi".equalsIgnoreCase(convertDocEntity.getPageNum().getType()) &&
                                convertDocEntity.getInputFiles().size() > 1) ||
                        // 如果设置为所有all，则加页码
                        ("all".equalsIgnoreCase(convertDocEntity.getPageNum().getType())) ||
                        // 如果不设置，则为所有，加页码
                        (convertDocEntity.getPageNum().getType() == null ||
                                "".equalsIgnoreCase(convertDocEntity.getPageNum().getType()))) {
                    intPageNum++;
                    intPageCount++;
                    convertDocEntity.getPageNum().addPageNum4Pdf(pdExtGfxState,
                            contentStream,
                            pdDocument,
                            page,
                            convertDocEntity.getPageNum(),
                            intPageNum,
                            intPageCount
                    );
                }
                // 否则，不加页码
            }

            // 如果添加【版权声明】，则进行如下处理
            if (convertDocEntity.getCopyRight() != null &&
                    convertDocEntity.getCopyRight().isEnable()) {
                // 如果设置为单个文件single，且输入文件也是单个，则加页码
                if (("single".equalsIgnoreCase(convertDocEntity.getCopyRight().getType()) &&
                        convertDocEntity.getInputFiles().size() == 1) ||
                        // 如果设置为多个文件mutli，且输入文件也是多个，则加页码
                        ("multi".equalsIgnoreCase(convertDocEntity.getCopyRight().getType()) &&
                                convertDocEntity.getInputFiles().size() > 1) ||
                        // 如果设置为所有all，则加页码
                        ("all".equalsIgnoreCase(convertDocEntity.getCopyRight().getType())) ||
                        // 如果不设置，则为所有，加页码
                        (convertDocEntity.getCopyRight().getType() == null ||
                                "".equalsIgnoreCase(convertDocEntity.getCopyRight().getType()))) {
                    intPageNum++;
                    intPageCount++;
                    convertDocEntity.getCopyRight().addCopyRight4Pdf(pdExtGfxState,
                            contentStream,
                            pdDocument,
                            page,
                            convertDocEntity.getCopyRight(),
                            intPageCount
                    );
                }
                // 否则，不加页码
            }

            // 如果添加文字水印，则进行如下处理
            if (convertDocEntity.getTextMark() != null) {
                convertDocEntity.getTextMark().mark4Pdf(pdExtGfxState,
                        contentStream,
                        pdDocument,
                        page,
                        convertDocEntity.getTextMark(),
                        modifyX,
                        convertDocEntity.getAlpha());
            }

            // 如果添加归档章水印，则进行如下处理
            if (i == 0 && convertDocEntity.getFirstPageMark() != null) {
                convertDocEntity.getFirstPageMark().mark4Pdf(pdExtGfxState,
                        contentStream,
                        pdDocument,
                        page,
                        convertDocEntity.getFirstPageMark(),
                        modifyX,
                        1f);
            }

            // 如果添加二维码/条码，则进行如下处理
            if (convertDocEntity.getBarCode() != null) {
                if (convertDocEntity.getBarCode().getIsFirstPage() && i > 0) {
                    continue;
                } else {
                    convertDocEntity.getBarCode().mark4Pdf(pdExtGfxState,
                            contentStream,
                            pdDocument,
                            page,
                            convertDocEntity.getBarCode(),
                            modifyX,
                            1f);
                }
            }

        }

        pdDocument.save(strTargetPdf);
    }

    /**
     * 勾股定理，通过斜边长度、角度，计算对边（勾）和临边（股）的长度
     *
     * @param bdXian    斜边（弦）长度
     * @param intDegree 斜边角度
     * @return Map对象，bdGou：对边（勾）长度；dbGu：临边（股）长度
     */
    public static Map<String, BigDecimal> getGouGu(BigDecimal bdXian, int intDegree) {
        Map<String, BigDecimal> mapReturn = new HashMap<>();

        // 角度转换为弧度制
        double dblRadians = Math.toRadians(intDegree);
        // 正弦值
        BigDecimal bdSin = BigDecimal.valueOf(Math.sin(dblRadians));
        // 四舍五入保留2位小数
        bdSin = bdSin.setScale(2, BigDecimal.ROUND_HALF_UP);
        // 对边（勾）长
        BigDecimal bdGou = bdXian.multiply(bdSin);
        mapReturn.put("bdGou", bdGou);

        //余弦值
        BigDecimal bdCos = BigDecimal.valueOf(Math.cos(dblRadians));
        //四舍五入保留2位小数
        bdCos = bdCos.setScale(2, BigDecimal.ROUND_HALF_UP);
        // 临边（股）长
        BigDecimal bdGu = bdXian.multiply(bdCos);
        mapReturn.put("bdGu", bdGu);

        return mapReturn;
    }

    /**
     * 根据单个文字的字号（斜边长度），计算文字宽度（临边长度）
     * 斜边（长边）、锐角，计算临边的长度
     *
     * @param hypotenuse     斜边（长边）的长度（例如：5.0）
     * @param angleInDegrees 锐角度数（例如：30.0）
     * @return 临边长度
     */
    public static double getFontWidth(double hypotenuse, double angleInDegrees) {
        // 将角度转换为弧度
        double angleInRadians = Math.toRadians(angleInDegrees);
        // 使用三角函数计算临边长度
        double value = hypotenuse * Math.cos(angleInRadians);

        return value;
    }

    /**
     * 根据单个文字的字号（斜边长度），计算文字高度（对边长度）
     * 斜边（长边）、锐角，计算对边的长度
     *
     * @param hypotenuse     斜边（长边）的长度（例如：5.0）
     * @param angleInDegrees 锐角度数（例如：30.0）
     * @return 对边长度
     */
    public static Double getFontHeight(double hypotenuse, double angleInDegrees) {
        // 将角度转换为弧度
        double angleInRadians = Math.toRadians(angleInDegrees);
        // 使用三角函数计算临边长度
        Double value = hypotenuse * Math.sin(angleInRadians);

        return value;
    }

    /**
     * 计算文本宽度
     */
    public static float getTextWidth(String text, float fontSize) {
        double doubleFontWidth = getFontWidth(fontSize, 30);
        return (float) doubleFontWidth * text.length();
    }

}
