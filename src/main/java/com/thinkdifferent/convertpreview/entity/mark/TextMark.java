package com.thinkdifferent.convertpreview.entity.mark;

import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.utils.FontUtil;
import com.thinkdifferent.convertpreview.utils.watermark.JpgWaterMarkUtil;
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
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文字水印
 */
@Log4j2
@Data
public class TextMark {
    /**
     * 水印内容
     */
    private String waterMarkText;
    /**
     * 旋转角度
     */
    private Integer degree;
    /**
     * 字号
     */
    private Integer fontSize;
    /**
     * 字体名称
     */
    private String fontName;
    /**
     * 字体颜色
     */
    private String fontColor;


    public TextMark get(Map<String, String> mapMark) {
        TextMark waterMarkText = new TextMark();
        // 水印文字
        waterMarkText.setWaterMarkText(MapUtil.getStr(mapMark, "waterMarkText"));
        // 旋转角度
        waterMarkText.setDegree(MapUtil.getInt(mapMark, "degree"));
        // 字体大小、名称、颜色
        waterMarkText.setFontSize(MapUtil.getInt(mapMark, "fontSize", 20));
        waterMarkText.setFontName(MapUtil.getStr(mapMark, "fontName", "宋体"));
        waterMarkText.setFontColor(MapUtil.getStr(mapMark, "fontColor", "gray"));

        return waterMarkText;

    }


    /**
     * PDF页面中添加文字水印
     *
     * @param pdExtGfxState
     * @param contentStream
     * @param pdDocument    PDF文档对象
     * @param page          PDF页面对象
     * @param textMark      文字水印对象
     * @param modifyX       矫正文字偏移
     * @param alpha         透明度
     * @throws Exception
     */
    public void mark4Pdf(PDExtendedGraphicsState pdExtGfxState,
                         PDPageContentStream contentStream,
                         PDDocument pdDocument,
                         PDPage page,
                         TextMark textMark,
                         float modifyX,
                         float alpha) throws Exception {
        pdExtGfxState.setNonStrokingAlphaConstant(alpha);
        pdExtGfxState.setAlphaSourceFlag(true);
        contentStream.setGraphicsStateParameters(pdExtGfxState);

        // 水印文字大小
        float floatFontSize = 15f;
        if (textMark.getFontSize() != null) {
            floatFontSize = (float) textMark.getFontSize();
        }

        // 页面高度 A4:841.92*595.32（磅）
        float floatPageHeightPt = page.getMediaBox().getHeight();
        // 页面宽度 A4:841.92*595.32（磅）
        float floatPageWidthPt = page.getMediaBox().getWidth();

        // 页面角度（横纵）（顺时针旋转角度）
        int rotation = page.getRotation();
        // 文字旋转角度
        int intDegree = textMark.getDegree();
        // 判断页面横纵。如果是横的，则调换宽高取值
        if (rotation > 0) {
            if(rotation == 90 || rotation == 270) {
                floatPageWidthPt = page.getMediaBox().getHeight();
                floatPageHeightPt = page.getMediaBox().getWidth();
            }

            if(rotation == 270){
                intDegree = - textMark.getDegree()*2;
            }else if(rotation == 180){
                intDegree = 270 - textMark.getDegree()*2;
            }else if(rotation == 90){
                intDegree = 180 - textMark.getDegree()*2;
            }else{
                intDegree = - textMark.getDegree();
            }
        }
        float maxPageSize = Math.max(floatPageWidthPt, floatPageHeightPt);
        // 如果纸张不是A4，则按照比例缩放字体大小
        float floatProportion = 1f;
        if(maxPageSize<841 || maxPageSize>842){
            floatProportion = (float)(maxPageSize/841.92);

            floatFontSize = floatProportion * floatFontSize;
        }

        // 利用勾股定理，依据字号，计算单个文字尺寸坐标
        Map<String, BigDecimal> mapGougu = PdfWaterMarkUtil.getGouGu(
                BigDecimal.valueOf(floatFontSize),
                textMark.getDegree()
        );
        // 对边
        BigDecimal bdFontHeight = mapGougu.get("bdGou");
        // 临边
        BigDecimal bdFontWidth = mapGougu.get("bdGu");

        // 使用"\n"将内容进行分割
        String[] strWaterMarkTexts = textMark.getWaterMarkText().split("\\n");
        List<String> listWaterMark = Arrays.asList(strWaterMarkTexts);

        // 水印文字字体
        String strFontFile = FontUtil.getSystemFontPathFile(ConvertDocConfigBase.waterMarkFont);
        PDFont pdfFont = PDType0Font.load(pdDocument, new FileInputStream(strFontFile), true);

        int maxSize = Math.max(listWaterMark.stream().mapToInt(String::length).max().orElse(10), 2 * listWaterMark.size());
        float markHeight = maxSize * bdFontHeight.floatValue() + 40*floatProportion;
        float markWidth = maxSize * bdFontWidth.floatValue() + 40*floatProportion;

        // 水印颜色
        Color color;
        if(textMark.getFontColor().startsWith("#")){
            color = Color.decode(textMark.getFontColor());
        }else{
            Field field = Color.class.getField(textMark.getFontColor());
            color = (Color) field.get(null);
        }
        contentStream.setNonStrokingColor(color);

        contentStream.beginText();
        // 设置字体大小
        contentStream.setFont(pdfFont, floatFontSize);

        // 宽
        for (int j = 0; j <= maxPageSize / markHeight; j++) {
            // 高
            for (int k = 0; k <= maxPageSize / markWidth; k++) {
                // 将分段的字段进行输出编写
                for (int z = 0; z < strWaterMarkTexts.length; z++) {
                    float tx = j * markHeight
                            + 3F * bdFontHeight.intValue() * (strWaterMarkTexts.length / 2 - z)
                            - modifyX;
                    float ty = k * markWidth
                            + 3F * bdFontWidth.intValue();

                    if(rotation == 90 || rotation == 270){
                        float floatTemp = tx;
                        tx = ty;
                        ty = floatTemp;
                    }

                    // 设置原点，并旋转坐标轴
                    contentStream.setTextMatrix(Matrix.getRotateInstance(
                            Math.toRadians(intDegree),
                            tx,
                            ty
                    ));

                    if(rotation == 0 || rotation ==90 ){
                        contentStream.showText(strWaterMarkTexts[strWaterMarkTexts.length - z - 1]);
                    }else if(rotation == 180 || rotation == 270){
                        contentStream.showText(strWaterMarkTexts[z]);
                    }
                }
            }
        }
        // 反向旋转
        contentStream.setTextMatrix(Matrix.getRotateInstance(
                Math.toRadians(-intDegree),
                0,
                0
        ));
        contentStream.endText();
        contentStream.restoreGraphicsState();
    }


    /**
     * 为OFD文件添加文字水印
     *
     * @param ofdDoc     OFD页面对象
     * @param pageSize   页面尺寸对象
     * @param textMark   文字水印对象
     * @param intPageNum 当前处理的页码
     * @param alpha      透明度
     * @throws Exception
     */
    public void mark4Ofd(OFDDoc ofdDoc, ST_Box pageSize,
                         TextMark textMark,
                         int intPageNum,
                         float alpha) throws Exception {
        // 水印倾斜角度（直角三角形的锐角度数）
        double doubleDegree = textMark.getDegree();
        // 使用"\n"将内容进行分割
        String strTextLine = textMark.getWaterMarkText();
        String[] strWaterMarkTexts = strTextLine.split("\\n");

        // 获取当前页纸张宽度。A4:297.015*210.019
        double doublePageWidthMm = pageSize.getWidth();
        // 获取当前页纸张高度。A4:297.015*210.019
        double doublePageHeightMm = pageSize.getHeight();
        // A4:297.015
        double maxPageSize = Math.max(doublePageWidthMm, doublePageHeightMm);

        // 字体大小
        int intFontSize = textMark.getFontSize() / 3;
        // 如果纸张不是A4，则按照比例缩放字体大小
        float floatProportion = 1f;
        if(maxPageSize<297 || maxPageSize>298){
            floatProportion = (float) (maxPageSize / 297);
            intFontSize = (int)floatProportion * intFontSize;
        }

        // 水印颜色
        Color color;
        if(textMark.getFontColor().startsWith("#")){
            color = Color.decode(textMark.getFontColor());
        }else{
            Field field = Color.class.getField(textMark.getFontColor());
            color = (Color) field.get(null);
        }

        // 水印文字字体
        FontSetting setting;
        if(StringUtils.isEmpty(ConvertDocConfigBase.waterMarkFont)){
            setting = new FontSetting(intFontSize, FontName.SimSun.font());
        }else{
            org.ofdrw.font.Font font = new org.ofdrw.font.Font(
                    ConvertDocConfigBase.waterMarkFontName,
                    ConvertDocConfigBase.waterMarkFontFamilyName,
                    Paths.get(ConvertDocConfigBase.waterMarkFont));
            setting = new FontSetting(intFontSize, font);
        }

        int finalIntFontSize = intFontSize;
        double finalDoubleFontLineHeightMm = Double.parseDouble(String.valueOf(intFontSize));
        Annotation annotation = new Annotation(
                new ST_Box(0d, 0d, doublePageWidthMm, doublePageHeightMm),
                AnnotType.Watermark, ctx -> {
            // 设置字体颜色、透明度
            ctx.setFont(setting);
            ctx.fillStyle="rgba("+color.getRed()+","+color.getGreen()+","+color.getBlue()+", "+alpha+")";
            // 获取多行文字的最大宽度
            double floatTextWidth = OfdWaterMarkUtil.getTextsWidth(strTextLine, finalIntFontSize);
            // 获取一个水印，在纸张中，旋转后的宽度
            double doubleOneMarkWidth = PdfWaterMarkUtil.getFontWidth( floatTextWidth, doubleDegree) * 2;
            // 获取一个水印，在纸张中，旋转后的高度
            double doubleOneMarkHeight = PdfWaterMarkUtil.getFontWidth( floatTextWidth, 90 - doubleDegree) * 2;

            // 生成列数（即向右填充的行数）
            for (int i = 0; i <= doublePageWidthMm / doubleOneMarkWidth; i++) {
                // 生成行数（即页面中横向水印的分组数量
                for (int j = 0; j < doublePageHeightMm / doubleOneMarkHeight; j++) {
                    // 输出每行文字
                    for (int line = 0; line < strWaterMarkTexts.length; line++) {
                        ctx.save();
                        // OFD文件坐标原点在【左上角】，文字旋转之后，必须将最左面的坐标点向下移动【doubleOneMarkHeight * (j+1)】，否则文字就输出到纸张顶部之外去了。
                        ctx.translate(doubleOneMarkWidth * i,
//                                doubleOneMarkHeight * (j*2+1));
                                doubleOneMarkHeight * (j+1));
                        ctx.rotate(-doubleDegree);
                        // 因为是输出多行，所以y坐标需要向下移动
                        ctx.fillText(strWaterMarkTexts[line],
                                finalDoubleFontLineHeightMm,
                                finalDoubleFontLineHeightMm * (line + 1));
                        ctx.restore();
                    }

                }
            }
        });

        ofdDoc.addAnnotation(intPageNum, annotation);
    }

    /**
     * 给JPG图片文件添加文字水印
     *
     * @param strInputJpg  输入的JPG文件路径和文件名
     * @param strOutPutJpg 输入的JPG文件路径和文件名
     * @param textMark     文字水印对象
     * @param alpha        透明度
     */
    public void mark4Jpg(String strInputJpg, String strOutPutJpg, TextMark textMark, float alpha) throws IOException {
        JpgWaterMarkUtil.markImageByText(textMark.waterMarkText,
                strInputJpg, strOutPutJpg,
                textMark.getDegree(),
                alpha,
                textMark.getFontSize(),
                textMark.getFontName(),
                textMark.getFontColor(),
                500, 500);
    }


}