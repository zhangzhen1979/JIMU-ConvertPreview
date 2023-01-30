package com.thinkdifferent.convertpreview.entity.mark;

import cn.hutool.core.map.MapUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.thinkdifferent.convertpreview.utils.WaterMarkUtil;
import com.thinkdifferent.convertpreview.utils.watermark.PdfWaterMarkUtil;
import lombok.extern.log4j.Log4j2;
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
import org.springframework.util.Assert;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文字水印
 */
@Log4j2
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

    public String getWaterMarkText() {
        return waterMarkText;
    }

    public void setWaterMarkText(String waterMarkText) {
        this.waterMarkText = waterMarkText;
    }

    public Integer getDegree() {
        return degree;
    }

    public void setDegree(Integer degree) {
        this.degree = degree;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }


    public TextMark get(Map<String, String> mapMark) {
        TextMark waterMarkText = new TextMark();
        // 水印文字
        waterMarkText.setWaterMarkText(MapUtil.getStr(mapMark, "waterMarkText"));
        Assert.hasText(waterMarkText.getWaterMarkText(), "文字水印内容不能为空");
        // 旋转角度
        waterMarkText.setDegree(MapUtil.getInt(mapMark, "degree"));
        // 字体大小、名称、颜色
        waterMarkText.setFontSize(MapUtil.getInt(mapMark, "fontSize", 40));
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
        contentStream.setGraphicsStateParameters(pdExtGfxState);

        // 水印文字大小
        float floatFontSize = 40f;
        if (textMark.getFontSize() != null) {
            floatFontSize = (float) textMark.getFontSize();
        }

        Map<String, BigDecimal> mapGougu = PdfWaterMarkUtil.getGouGu(
                BigDecimal.valueOf(textMark.getFontSize()),
                textMark.getDegree()
        );
        // 对边
        BigDecimal bdFontHeight = mapGougu.get("bdGou");
        // 临边
        BigDecimal bdFontWidth = mapGougu.get("bdGu");

        // 使用"||"将内容进行分割
        String[] strWaterMarkTexts = textMark.getWaterMarkText().split("\\|\\|");
        List<String> listWaterMark = Arrays.asList(strWaterMarkTexts);

        // 水印文字字体
        PDFont pdfFont = PDType0Font.load(pdDocument, new FileInputStream(SpringUtil.getProperty("convert.pdf.font")), true);
        int maxSize = Math.max(listWaterMark.stream().mapToInt(String::length).max().orElse(10), 2 * listWaterMark.size());
        float markHeight = maxSize * bdFontHeight.floatValue() + 40;
        float markWidth = maxSize * bdFontWidth.floatValue() + 40;

        // 水印颜色
        Field field = Color.class.getField(textMark.getFontColor());
        Color color = (Color) field.get(null);
        contentStream.setNonStrokingColor(color);

        contentStream.beginText();
        // 设置字体大小
        contentStream.setFont(pdfFont, floatFontSize);

        int rotation = page.getRotation();
        float floatPageHeight = page.getMediaBox().getHeight();
        float floatPageWidth = page.getMediaBox().getWidth();
        if (rotation > 0) {
            floatPageWidth = page.getMediaBox().getHeight();
            floatPageHeight = page.getMediaBox().getWidth();
        }
        float maxPage = Math.max(floatPageWidth, floatPageHeight);
        // 宽
        for (int j = 0; j <= maxPage / markHeight; j++) {
            // 高
            for (int k = 0; k <= maxPage / markWidth; k++) {
                // 将分段的字段进行输出编写
                for (int z = 0; z < strWaterMarkTexts.length; z++) {
                    float tx = j * markHeight
                            + 3F * bdFontHeight.intValue() * (strWaterMarkTexts.length / 2 - z)
                            - modifyX;
                    float ty = k * markWidth + 3F * bdFontWidth.intValue();
                    contentStream.setTextMatrix(Matrix.getRotateInstance(
                            Math.toRadians(textMark.getDegree()),
                            tx,
                            ty
                    ));
                    contentStream.showText(strWaterMarkTexts[strWaterMarkTexts.length - z - 1]);
                }
            }
        }
        // 反向旋转
        contentStream.setTextMatrix(Matrix.getRotateInstance(
                Math.toRadians(-textMark.getDegree()),
                0,
                0
        ));
        contentStream.endText();
        contentStream.restoreGraphicsState();

    }

    /**
     * PDF页面中添加页码水印
     *
     * @param pdExtGfxState
     * @param contentStream
     * @param pdDocument    PDF文档对象
     * @param page          PDF页面对象
     * @param intPageNum    页码数字
     * @throws Exception
     */
    public void addPageNum4Pdf(PDExtendedGraphicsState pdExtGfxState,
                               PDPageContentStream contentStream,
                               PDDocument pdDocument,
                               PDPage page,
                               int intPageNum) throws Exception {
        pdExtGfxState.setNonStrokingAlphaConstant(1f);
        pdExtGfxState.setAlphaSourceFlag(true);
        contentStream.setGraphicsStateParameters(pdExtGfxState);

        // 水印文字大小
        float floatFontSize = 15f;
        // 水印文字字体
        PDFont pdfFont = PDType0Font.load(pdDocument, new FileInputStream(SpringUtil.getProperty("convert.pdf.font")), true);
        // 水印颜色
        Field field = Color.class.getField("black");
        Color color = (Color) field.get(null);
        contentStream.setNonStrokingColor(color);

        contentStream.beginText();
        // 设置字体大小
        contentStream.setFont(pdfFont, floatFontSize);

        int rotation = page.getRotation();
        float floatPageWidth = page.getMediaBox().getWidth();
        float floatPageHeight = page.getMediaBox().getHeight();
        if (rotation > 0) {
            floatPageWidth = page.getMediaBox().getHeight();
            floatPageHeight = page.getMediaBox().getWidth();
        }

        // 计算页面右上角位置
        float floatTx = floatPageWidth - 30f;
        float floatTy = floatPageHeight - 30f;

        // 在右上角加入页码文字水印
        contentStream.setTextMatrix(Matrix.getRotateInstance(
                Math.toRadians(0),
                floatTx,
                floatTy
        ));
        contentStream.showText(String.valueOf(intPageNum));
        // 反向旋转
        contentStream.setTextMatrix(Matrix.getRotateInstance(
                Math.toRadians(0),
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
     * @param h          文字高度
     * @param alpha      透明度
     * @throws Exception
     */
    public void mark4Ofd(OFDDoc ofdDoc, ST_Box pageSize,
                         TextMark textMark,
                         int intPageNum,
                         float h,
                         float alpha) throws Exception {

        double dblPageWidth = pageSize.getWidth();
        double dblPageHeight = pageSize.getHeight();
        double dblBorderLength = Math.max(dblPageHeight, dblPageWidth);
        double dblMaxLength = dblBorderLength * 2;

        // 水印颜色
        Field field = Color.class.getField(textMark.getFontColor());
        Color color = (Color) field.get(null);
        // 使用"||"将内容进行分割
        String[] strWaterMarkTexts = textMark.getWaterMarkText().split("\\|\\|");

        // 声明每页上需要绘制的水印，以及水印位置
        Annotation annotation = new Annotation(
                new ST_Box(0d, 0d,
                        dblMaxLength, dblMaxLength),
                AnnotType.Watermark, ctx -> {
            FontSetting setting = new FontSetting(textMark.getFontSize() / 2, FontName.SimSun.font());

            ctx.setFillColor(color.getRed(), color.getGreen(), color.getBlue())
                    .setFont(setting)
                    .setGlobalAlpha((double) alpha);
            // 两行水印的间距， 默认高度一半
            double dblLineHeight = Math.max(textMark.getFontSize() * 1.5 * strWaterMarkTexts.length, h / 2);
            //对ofd页面填充4行10列的水印，并顺时针旋转45°
            for (int i = 0; i <= dblMaxLength / h; i++) {// 行数
                for (int j = 0; j <= dblMaxLength / dblLineHeight; j++) { // 列数
                    // 将分段的字段进行输出编写
                    for (int z = 0; z < strWaterMarkTexts.length; z++) {
                        ctx.save();
                        ctx.rotate(-1 * textMark.getDegree());
                        // 以x,y为原点, x左移页面一半，y为矩形高度
                        ctx.translate(i * h - dblPageWidth / 2, j * dblLineHeight);
                        ctx.fillText(strWaterMarkTexts[z],
                                z * textMark.getFontSize(),
                                z * textMark.getFontSize()
                        );
                        ctx.restore();
                    }
                }
            }
        });

        ofdDoc.addAnnotation(intPageNum, annotation);
    }


    /**
     * OFD页面中添加页码水印
     *
     * @param ofdDoc     OFD页面对象
     * @param pageSize   页面尺寸对象
     * @param i          当前页
     * @param intPageNum 当前处理的页码
     * @throws Exception
     */
    public void addPageNum4Ofd(OFDDoc ofdDoc, ST_Box pageSize,
                               int i,
                               int intPageNum) throws NoSuchFieldException, IllegalAccessException, IOException {

        double dblPageWidth = pageSize.getWidth();
        double dblPageHeight = pageSize.getHeight();
        double dblBorderLength = Math.max(dblPageHeight, dblPageWidth);
        // 计算页面右下角位置
        double doubleX = dblPageWidth - 10;
        double doubleY = 10d;

        // 水印颜色
        Field field = Color.class.getField("black");
        Color color = (Color) field.get(null);

        // 声明每页上需要绘制的水印，以及水印位置
        Annotation annotation = new Annotation(
                new ST_Box(0d, 0d,
                        dblBorderLength, dblBorderLength),
                AnnotType.Watermark, ctx -> {
            FontSetting setting = new FontSetting(5f, FontName.SimSun.font());

            ctx.setFillColor(color.getRed(), color.getGreen(), color.getBlue())
                    .setFont(setting)
                    .setGlobalAlpha(1d);
            ctx.save();
            ctx.rotate(0d);
            ctx.fillText(String.valueOf(intPageNum),
                    doubleX,
                    doubleY);
            ctx.restore();
        });

        ofdDoc.addAnnotation(i, annotation);
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
        WaterMarkUtil.markImageByText(textMark.waterMarkText,
                strInputJpg, strOutPutJpg,
                textMark.getDegree(),
                alpha,
                textMark.getFontSize(),
                textMark.getFontName(),
                textMark.getFontColor(),
                500, 500);
    }


}