package com.thinkdifferent.convertpreview.utils.watermark;

import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
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
     *  @param strSourcePdf  源PDF
     * @param strTargetPdf  目标PDF
     * @param convertEntity 转换参数
     * @param intPageNum    上一个文件的最后页码
     */
    public static void mark4Pdf(String strSourcePdf,
                                String strTargetPdf,
                                ConvertEntity convertEntity,
                                int intPageNum) throws Exception{
        File fileInputPdf = new File(strSourcePdf);
        strTargetPdf = SystemUtil.beautifulFilePath(strTargetPdf);

        //打开pdf文件
        @Cleanup PDDocument pdDocument = PDDocument.load(fileInputPdf);
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
            if (convertEntity.getPngMark() != null) {
                convertEntity.getPngMark().mark4Pdf(pdExtGfxState,
                        contentStream,
                        pdDocument,
                        page,
                        convertEntity.getPngMark(),
                        0F,
                        false,
                        convertEntity.getAlpha());

                modifyX = convertEntity.getPngMark().getLocateX() == 0f ? 150F : convertEntity.getPngMark().getLocateX();
            }

            // 如果添加页码，则进行如下处理
            if(convertEntity.isPageNum()){
                intPageNum++;
                convertEntity.getTextMark().addPageNum4Pdf(pdExtGfxState,
                        contentStream,
                        pdDocument,
                        page,
                        intPageNum);
            }

            // 如果添加文字水印，则进行如下处理
            if (convertEntity.getTextMark() != null) {
                convertEntity.getTextMark().mark4Pdf(pdExtGfxState,
                        contentStream,
                        pdDocument,
                        page,
                        convertEntity.getTextMark(),
                        modifyX,
                        convertEntity.getAlpha());
            }

            // 如果添加归档章水印，则进行如下处理
            if (i == 0 && convertEntity.getFirstPageMark() != null) {
                convertEntity.getFirstPageMark().mark4Pdf(pdExtGfxState,
                        contentStream,
                        pdDocument,
                        page,
                        convertEntity.getFirstPageMark(),
                        modifyX,
                        1f);
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

}
