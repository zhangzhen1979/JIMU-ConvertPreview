package com.thinkdifferent.convertpreview.entity.mark;

import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.utils.FontUtil;
import com.thinkdifferent.convertpreview.utils.watermark.OfdWaterMarkUtil;
import com.thinkdifferent.convertpreview.utils.watermark.PdfWaterMarkUtil;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.edit.Annotation;
import org.ofdrw.layout.element.canvas.FontSetting;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 版权声明
 */
@Log4j2
@Data
public class CopyRight {
    /**
     * 是否启用【版权声明】
     */
    private boolean enable;
    /**
     * 类型
     */
    private String type;
    /**
     * 文字内容
     */
    private String text;
    /**
     * 字号
     */
    private Float fontSize;
    /**
     * 字体颜色
     */
    private String fontColor;
    /**
     * 页码位置
     */
    private String locate;
    /**
     * 页码边距
     */
    private Integer margins;
    /**
     * 是否奇偶页，页码位置互换
     */
    private boolean swapPosition;
    /**
     * 起始页码（PDF、OFD有效）
     */
    private Integer startNum;


    public CopyRight get(Map<String, String> mapMark) {
        CopyRight copyRight = new CopyRight();
        copyRight.setText(MapUtil.getStr(mapMark, "text", "CopyRight").toLowerCase());
        // 字体大小、颜色、位置
        copyRight.setFontSize(MapUtil.getFloat(mapMark, "fontSize", 15f));
        copyRight.setFontColor(MapUtil.getStr(mapMark, "fontColor", "black").toLowerCase());
        copyRight.setLocate(MapUtil.getStr(mapMark, "locate", "TR"));
        copyRight.setMargins(MapUtil.getInt(mapMark, "margins", 30));
        copyRight.setSwapPosition(MapUtil.getBool(mapMark, "swapPosition", false));
        copyRight.setStartNum(MapUtil.getInt(mapMark, "startNum", 1));

        return copyRight;
    }


    /**
     * PDF页面中添加【版权说明】水印
     *
     * @param pdExtGfxState
     * @param contentStream
     * @param pdDocument    PDF文档对象
     * @param page          PDF页面对象
     * @param copyRight     【版权说明】对象
     * @param intPageCount  截止到上一个文件的最后总页数
     * @throws Exception
     */
    public void addCopyRight4Pdf(PDExtendedGraphicsState pdExtGfxState,
                               PDPageContentStream contentStream,
                               PDDocument pdDocument,
                               PDPage page,
                               CopyRight copyRight,
                               int intPageCount) throws Exception {
        // 如果启用了【版权说明】，才加【版权说明】水印；否则不处理。
        if(copyRight.isEnable()){
            pdExtGfxState.setNonStrokingAlphaConstant(1f);
            pdExtGfxState.setAlphaSourceFlag(true);
            contentStream.setGraphicsStateParameters(pdExtGfxState);

            float floatMargins = margins;

            // 水印文字大小
            float floatFontSize = 15f;
            if(copyRight.getFontSize() != null){
                floatFontSize = copyRight.getFontSize();
            }

            // 页面高度 A4:841.92*595.32（磅）
            float floatPageHeightPt = page.getMediaBox().getHeight();
            // 页面宽度 A4:841.92*595.32（磅）
            float floatPageWidthPt = page.getMediaBox().getWidth();

            String strLocate = copyRight.getLocate().toUpperCase();
            // 页面角度（横纵）
            int rotation = page.getRotation();
            // 判断页面横纵。
            if(rotation == 90){
                switch (strLocate) {
                    case "TL":
                        strLocate = "BL";
                        break;
                    case "TM":
                        strLocate = "CL";
                        break;
                    case "TR":
                        strLocate = "TL";
                        break;
                    case "CL":
                        strLocate = "BM";
                        break;
                    case "CR":
                        strLocate = "TM";
                        break;
                    case "BL":
                        strLocate = "BR";
                        break;
                    case "BM":
                        strLocate = "CR";
                        break;
                    case "BR":
                        strLocate = "TR";
                }
            }
            if(rotation == 270){
                switch (strLocate) {
                    case "TL":
                        strLocate = "TR";
                        break;
                    case "TM":
                        strLocate = "CR";
                        break;
                    case "TR":
                        strLocate = "BR";
                        break;
                    case "CL":
                        strLocate = "TM";
                        break;
                    case "CR":
                        strLocate = "BM";
                        break;
                    case "BL":
                        strLocate = "TL";
                        break;
                    case "BM":
                        strLocate = "CL";
                        break;
                    case "BR":
                        strLocate = "BL";
                }
            }

            float maxPageSize = Math.max(floatPageWidthPt, floatPageHeightPt);
            // 如果纸张不是A4，则按照比例缩放字体大小
            float floatProportion = 1f;
            if(maxPageSize<841 || maxPageSize>842){
                floatProportion = (float)(maxPageSize/841.92);

                floatFontSize = floatProportion * floatFontSize;
                floatMargins = floatProportion * floatMargins;
            }

            // 水印文字字体
            String strFontFile = FontUtil.getSystemFontPathFile(ConvertDocConfigBase.waterMarkFont);
            PDFont pdfFont = PDType0Font.load(pdDocument, new FileInputStream(strFontFile), true);

            // 水印颜色
            Color color;
            if(copyRight.getFontColor().startsWith("#")){
                color = Color.decode(copyRight.getFontColor());
            }else{
                Field field = Color.class.getField(copyRight.getFontColor());
                color = (Color) field.get(null);
            }
            contentStream.setNonStrokingColor(color);

            contentStream.beginText();
            // 设置字体大小
            contentStream.setFont(pdfFont, floatFontSize);

            // 计算页面右上角位置（默认位置）
            float floatTx = floatPageWidthPt - floatMargins;
            float floatTy = floatPageHeightPt - floatMargins;

            // 判断是否需要奇偶页位置互换
            if(copyRight.swapPosition && intPageCount % 2 == 0){
                switch (strLocate){
                    case "TL":
                        strLocate = "TR";
                        break;
                    case "TR":
                        strLocate = "TL";
                        break;
                    case "CL":
                        strLocate = "CR";
                        break;
                    case "CR":
                        strLocate = "CL";
                        break;
                    case "BL":
                        strLocate = "BR";
                        break;
                    case "BR":
                        strLocate = "BL";
                }
            }

            // 根据传入的【位置】参数，计算页码在页面中的绝对位置。（距离边缘30f）
            switch (strLocate){
                case "TL":
                case "TM":
                case "TR":
                    floatTy = floatPageHeightPt - floatMargins;
                    break;
                case "CL":
                case "C":
                case "CR":
                    floatTy = (float)(floatPageHeightPt * 0.5);
                    break;
                case "BL":
                case "BM":
                case "BR":
                    floatTy = floatMargins;
                    break;
            }

            String[] strTexts = copyRight.text.split("\\n");
            switch (strLocate){
                case "BL":
                case "BM":
                case "BR":
                    floatTy = floatTy + floatFontSize * (strTexts.length-1);
            }

            for (String strTextLine : strTexts) {
                if (!strTextLine.equals("")) {
                    float floatTextWidth = PdfWaterMarkUtil.getTextWidth(strTextLine, floatFontSize);

                    // 根据传入的【位置】参数，计算页码在页面中的绝对位置。（距离边缘30f）
                    switch (strLocate){
                        case "TL":
                        case "CL":
                        case "BL":
                            floatTx = floatMargins;
                            break;
                        case "TM":
                        case "C":
                        case "BM":
                            floatTx = (float)((floatPageWidthPt - floatTextWidth) * 0.5);
                            break;
                        case "TR":
                        case "CR":
                        case "BR":
                            floatTx = floatPageWidthPt - floatTextWidth - floatMargins;
                            break;
                    }


                    if(rotation == 90){
                        switch (copyRight.getLocate().toUpperCase()) {
                            case "TL":
                                floatTx = floatMargins;
                                break;
                            case "TM":
                                floatTx = floatMargins;
                                floatTy = floatPageHeightPt/2 - floatMargins/2 - floatTextWidth/2;
                                break;
                            case "TR":
                                floatTx = floatMargins;
                                floatTy = floatPageHeightPt - floatTextWidth - floatMargins;
                                break;
                            case "CL":
                                break;
                            case "C":
                                floatTy = floatPageHeightPt/2 - floatTextWidth/2;
                                break;
                            case "CR":
                                floatTy = floatPageHeightPt - floatTextWidth - floatMargins;
                                break;
                            case "BL":
                                floatTx = floatPageWidthPt - floatMargins;
                                break;
                            case "BM":
                                floatTx = floatPageWidthPt - floatMargins;
                                floatTy = floatPageHeightPt/2 - floatMargins/2 - floatTextWidth/2;
                                break;
                            case "BR":
                                floatTx = floatPageWidthPt - floatMargins;
                                floatTy = floatPageHeightPt - floatTextWidth - floatMargins;
                        }
                    }
                    if(rotation == 270){
                        switch (copyRight.getLocate().toUpperCase()) {
                            case "TL":
                                floatTx = floatPageWidthPt - floatMargins;
                                break;
                            case "TM":
                                floatTx = floatPageWidthPt - floatMargins;
                                floatTy = floatPageHeightPt/2 - floatMargins/2 + floatTextWidth/2;
                                break;
                            case "TR":
                                floatTx = floatPageWidthPt - floatMargins;
                                floatTy = floatTextWidth + floatMargins;
                                break;
                            case "CL":
                                floatTx = floatPageWidthPt/2 - floatMargins/2;
                                break;
                            case "C":
                                floatTx = floatPageWidthPt/2 - floatMargins/2;
                                floatTy = floatPageHeightPt/2 - floatMargins/2 + floatTextWidth/2;
                                break;
                            case "CR":
                                floatTx = floatPageWidthPt/2 - floatMargins/2;
                                floatTy = floatTextWidth + floatMargins;
                                break;
                            case "BL":
                                floatTx = floatMargins;
                                break;
                            case "BM":
                                floatTx = floatMargins;
                                floatTy = floatPageHeightPt/2 - floatMargins/2 + floatTextWidth/2;
                                break;
                            case "BR":
                                floatTx = floatMargins;
                                floatTy = floatTextWidth + floatMargins;
                        }
                    }

                    // 移动坐标原点，准备添加文字。
                    // 通过Matrix.getRotateInstance(angle, x, y)进行旋转会围绕坐标系的原点(0，0)旋转(通常是页面的左下角)，然后平移(x，y)。
                    contentStream.setTextMatrix(Matrix.getRotateInstance(
                            Math.toRadians(rotation),
                            floatTx,
                            floatTy
                    ));
                    // 加入文字水印
                    contentStream.showText(strTextLine);

                    floatTy = floatTy - floatFontSize;

                    // 反向旋转
                    contentStream.setTextMatrix(Matrix.getRotateInstance(
                            Math.toRadians(-rotation),
                            0,
                            0
                    ));
                }
            }

            contentStream.endText();
            contentStream.restoreGraphicsState();
        }

    }

    /**
     * OFD页面中添加【版权说明】水印
     *
     * @param ofdDoc     OFD页面对象
     * @param pageSize   页面尺寸对象
     * @param copyRight  【版权说明】对象
     * @param i          当前页
     * @param intPageCount  截止到上一个文件的最后总页数
     * @throws Exception
     */
    public void addCopyRight4Ofd(OFDDoc ofdDoc, ST_Box pageSize,
                               CopyRight copyRight,
                               int i,
                               int intPageCount)
            throws NoSuchFieldException, IllegalAccessException, IOException {
        // 如果启用了【版权说明】，才加【版权说明】水印；否则不处理。
        if(copyRight.isEnable()){
            // 计算页面右上角位置（默认位置）
            double doubleMargins = (double)margins / 3;

            // 获取当前页纸张宽度。A4:297.015*210.019
            double doublePageWidthMm = pageSize.getWidth();
            // 获取当前页纸张高度。A4:297.015*210.019
            double doublePageHeightMm = pageSize.getHeight();
            // A4:297.015
            double maxPageSize = Math.max(doublePageWidthMm, doublePageHeightMm);

            // 字体大小
            float floatFontSize = copyRight.getFontSize() / 3;
            // 如果纸张不是A4，则按照比例缩放字体大小
            float floatProportion = 1f;
            if(maxPageSize<297 || maxPageSize>298){
                floatProportion = (float) (maxPageSize / 297);
                floatFontSize = floatProportion * floatFontSize;
                doubleMargins = floatProportion * doubleMargins;
            }

            // 水印颜色
            Color color;
            if(copyRight.getFontColor().startsWith("#")){
                color = Color.decode(copyRight.getFontColor());
            }else{
                Field field = Color.class.getField(copyRight.getFontColor());
                color = (Color) field.get(null);
            }

            // 水印文字字体
            FontSetting setting;
            if(StringUtils.isEmpty(ConvertDocConfigBase.waterMarkFont)){
                setting = new FontSetting(floatFontSize, FontName.SimSun.font());
            }else{
                org.ofdrw.font.Font font = new org.ofdrw.font.Font(
                        ConvertDocConfigBase.waterMarkFontName,
                        ConvertDocConfigBase.waterMarkFontFamilyName,
                        Paths.get(ConvertDocConfigBase.waterMarkFont));
                setting = new FontSetting(floatFontSize, font);
            }

            // 水印文字内容
            String strText = copyRight.text;

            // 水印位置
            String strLocate = copyRight.getLocate().toUpperCase();
            // 判断是否需要奇偶页位置互换
            if(copyRight.swapPosition && intPageCount % 2 == 0){
                switch (strLocate){
                    case "TL":
                        strLocate = "TR";
                        break;
                    case "TR":
                        strLocate = "TL";
                        break;
                    case "CL":
                        strLocate = "CR";
                        break;
                    case "CR":
                        strLocate = "CL";
                        break;
                    case "BL":
                        strLocate = "BR";
                        break;
                    case "BR":
                        strLocate = "BL";
                }
            }

            // 声明每页上需要绘制的水印，以及水印位置
            String finalStrLocate = strLocate;
            double finalDoubleMargins = doubleMargins;
            float finalFloatFontSize = floatFontSize;
            Annotation annotation = new Annotation(
                    new ST_Box(0d, 0d,
                            doublePageWidthMm, doublePageHeightMm),
                    AnnotType.Watermark, ctx -> {
                // 设置字体颜色
                ctx.setFont(setting);
                ctx.fillStyle="rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+")";

                Double finalDoubleX = doublePageWidthMm;
                Double finalDoubleY = finalDoubleMargins;
                // 根据传入的【位置】参数，计算页码在页面中的绝对位置。（距离边缘10d）
                switch (finalStrLocate){
                    case "TL":
                    case "TM":
                    case "TR":
                        finalDoubleY = finalDoubleMargins;
                        break;
                    case "CL":
                    case "C":
                    case "CR":
                        finalDoubleY = doublePageHeightMm * 0.5;
                        break;
                    case "BL":
                    case "BM":
                    case "BR":
                        finalDoubleY = doublePageHeightMm - finalDoubleMargins;
                }

                String[] strTexts = strText.split("\\n");
                switch (finalStrLocate){
                    case "BL":
                    case "BM":
                    case "BR":
                        finalDoubleY = finalDoubleY - finalFloatFontSize * (strTexts.length-1);
                        break;
                    default:
                        finalDoubleY = finalDoubleY + finalFloatFontSize * (strTexts.length-1);
                }

                for (String strTextLine : strTexts) {
                    if (!strTextLine.equals("")) {
                        Double doubleTextWidth = OfdWaterMarkUtil.getTextWidth(strTextLine, finalFloatFontSize);
                        // 根据传入的【位置】参数，计算页码在页面中的绝对位置。（距离边缘30f）
                        switch (finalStrLocate){
                            case "TL":
                            case "CL":
                            case "BL":
                                finalDoubleX = finalDoubleMargins;
                                break;
                            case "TM":
                            case "C":
                            case "BM":
                                finalDoubleX = doublePageWidthMm * 0.5 - doubleTextWidth * 0.5;
                                break;
                            case "TR":
                            case "CR":
                            case "BR":
                                finalDoubleX = doublePageWidthMm - doubleTextWidth - finalDoubleMargins * 2;
                                break;
                        }

                        ctx.save();
                        ctx.rotate(0d);
                        ctx.fillText(strTextLine,
                                finalDoubleX,
                                finalDoubleY);

                        switch (finalStrLocate){
                            case "BL":
                            case "BM":
                            case "BR":
                                finalDoubleY = finalDoubleY + finalFloatFontSize;
                                break;
                            default:
                                finalDoubleY = finalDoubleY - finalFloatFontSize;
                        }
                    }
                }
                ctx.restore();
            });
            ofdDoc.addAnnotation(i, annotation);
        }

    }

}