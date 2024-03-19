package com.thinkdifferent.convertpreview.entity.mark;

import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.utils.DataUtil;
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
 * 页码
 */
@Log4j2
@Data
public class PageNum {
    /**
     * 是否启用页码
     */
    private boolean enable;
    /**
     * 类型
     */
    private String type;
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
    /**
     * 自动补零位数。如果不为0，则自动补0；如果为0，则不补0。
     */
    private Integer digits;


    public PageNum get(Map<String, String> mapMark) {
        PageNum pageNum = new PageNum();
        // 字体大小、颜色、位置
        pageNum.setFontSize(MapUtil.getFloat(mapMark, "fontSize", 15f));
        pageNum.setFontColor(MapUtil.getStr(mapMark, "fontColor", "black").toLowerCase());
        pageNum.setLocate(MapUtil.getStr(mapMark, "locate", "TR"));
        pageNum.setMargins(MapUtil.getInt(mapMark, "margins", 30));
        pageNum.setSwapPosition(MapUtil.getBool(mapMark, "swapPosition", false));
        pageNum.setStartNum(MapUtil.getInt(mapMark, "startNum", 1));
        pageNum.setDigits(MapUtil.getInt(mapMark, "digits", 0));

        return pageNum;
    }


    /**
     * PDF页面中添加页码水印
     *
     * @param pdExtGfxState
     * @param contentStream
     * @param pdDocument    PDF文档对象
     * @param page          PDF页面对象
     * @param pageNum       页码对象
     * @param intPageNum    当前处理的页码
     * @param intPageCount  截止到上一个文件的最后总页数
     * @throws Exception
     */
    public void addPageNum4Pdf(PDExtendedGraphicsState pdExtGfxState,
                               PDPageContentStream contentStream,
                               PDDocument pdDocument,
                               PDPage page,
                               PageNum pageNum,
                               int intPageNum,
                               int intPageCount) throws Exception {
        // 如果启用了【页码】，才加页码水印；否则不处理。
        if(pageNum.isEnable()){
            pdExtGfxState.setNonStrokingAlphaConstant(1f);
            pdExtGfxState.setAlphaSourceFlag(true);
            contentStream.setGraphicsStateParameters(pdExtGfxState);

            float floatMargins = margins;

            // 水印文字大小
            float floatFontSize = 15f;
            if(pageNum.getFontSize() != null){
                floatFontSize = pageNum.getFontSize();
            }

            // 页面高度 A4:841.92*595.32（磅）
            float floatPageHeightPt = page.getMediaBox().getHeight();
            // 页面宽度 A4:841.92*595.32（磅）
            float floatPageWidthPt = page.getMediaBox().getWidth();

            String strLocate = pageNum.getLocate().toUpperCase();
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
            if(pageNum.getFontColor().startsWith("#")){
                color = Color.decode(pageNum.getFontColor());
            }else{
                Field field = Color.class.getField(pageNum.getFontColor());
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
            if(pageNum.swapPosition && intPageCount % 2 == 0){
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

            float floatTextWidth = PdfWaterMarkUtil.getTextWidth(String.valueOf(intPageNum), floatFontSize);

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
                    floatTx = (float)(floatPageWidthPt * 0.5  - floatTextWidth * 0.5);
                    break;
                case "TR":
                case "CR":
                case "BR":
                    floatTx = floatPageWidthPt - floatTextWidth - floatMargins;
                    break;
            }

            // 移动坐标原点，准备添加文字。
            // 通过Matrix.getRotateInstance(angle, x, y)进行旋转会围绕坐标系的原点(0，0)旋转(通常是页面的左下角)，然后平移(x，y)。
            contentStream.setTextMatrix(Matrix.getRotateInstance(
                    Math.toRadians(rotation),
                    floatTx,
                    floatTy
            ));
            // 判断是否需要为页码【补0】
            String strPageNum = DataUtil.autoAddZero(intPageNum, pageNum.getDigits());
            // 加入页码文字
            contentStream.showText(strPageNum);
            // 反向旋转
            contentStream.setTextMatrix(Matrix.getRotateInstance(
                    Math.toRadians(-rotation),
                    0,
                    0
            ));

            contentStream.endText();
            contentStream.restoreGraphicsState();
        }

    }

    /**
     * OFD页面中添加页码水印
     *
     * @param ofdDoc     OFD页面对象
     * @param pageSize   页面尺寸对象
     * @param pageNum       页码对象
     * @param i          当前页
     * @param intPageNum 当前处理的页码
     * @param intPageCount  截止到上一个文件的最后总页数
     * @throws Exception
     */
    public void addPageNum4Ofd(OFDDoc ofdDoc, ST_Box pageSize,
                               PageNum pageNum,
                               int i,
                               int intPageNum,
                               int intPageCount) throws NoSuchFieldException, IllegalAccessException, IOException {
        // 如果启用了【页码】，才加页码水印；否则不处理。
        if(pageNum.isEnable()){
            // 计算页面右上角位置（默认位置）
            double doubleMargins = (double)margins / 3;

            // 获取当前页纸张宽度。A4:297.015*210.019
            double doublePageWidthMm = pageSize.getWidth();
            // 获取当前页纸张高度。A4:297.015*210.019
            double doublePageHeightMm = pageSize.getHeight();
            // A4:297.015
            double maxPageSize = Math.max(doublePageWidthMm, doublePageHeightMm);

            // 字体大小
            float floatFontSize = pageNum.getFontSize() / 3;
            // 如果纸张不是A4，则按照比例缩放字体大小
            float floatProportion = 1f;
            if(maxPageSize<297 || maxPageSize>298){
                floatProportion = (float) (maxPageSize / 297);
                floatFontSize = floatProportion * floatFontSize;
                doubleMargins = floatProportion * doubleMargins;
            }

            // 水印颜色
            Color color;
            if(pageNum.getFontColor().startsWith("#")){
                color = Color.decode(pageNum.getFontColor());
            }else{
                Field field = Color.class.getField(pageNum.getFontColor());
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

            String strLocate = pageNum.getLocate().toUpperCase();
            // 判断是否需要奇偶页位置互换
            if(pageNum.swapPosition && intPageCount % 2 == 0){
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
            float finalFloatFontSize = floatFontSize;
            double finalDoubleMargins = doubleMargins;
            Annotation annotation = new Annotation(
                    new ST_Box(0d, 0d,
                            doublePageWidthMm, doublePageHeightMm),
                    AnnotType.Watermark, ctx -> {
                // 设置字体颜色
                ctx.setFont(setting);
                ctx.fillStyle="rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+")";

                double finalDoubleX = doublePageWidthMm;
                double finalDoubleY = finalDoubleMargins;
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

                double floatTextWidth = OfdWaterMarkUtil.getTextWidth(String.valueOf(intPageNum), finalFloatFontSize);

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
                        finalDoubleX = doublePageWidthMm * 0.5 - floatTextWidth * 0.5;
                        break;
                    case "TR":
                    case "CR":
                    case "BR":
                        finalDoubleX = doublePageWidthMm - floatTextWidth - finalDoubleMargins;
                        break;
                }

                ctx.save();
                ctx.rotate(0d);
                // 判断是否需要为页码【补0】
                String strPageNum = DataUtil.autoAddZero(intPageNum, pageNum.getDigits());
                // 加入页码文字
                ctx.fillText(strPageNum,
                        finalDoubleX,
                        finalDoubleY);
                ctx.restore();
            });
            ofdDoc.addAnnotation(i, annotation);
        }

    }

}