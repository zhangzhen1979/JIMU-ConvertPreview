package com.thinkdifferent.convertoffice.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WaterMarkUtil {

    /**
     * @param args
     */
    public static void main(String[] args) {
//        String strSourcePdfPath = "d:/1.pdf";
//        String strTargetPdfPath = "d:/cvtest/1-water.pdf";
        // 添加水印
//        String iconPath = "d:/watermark.png";
//        WaterMarkUtil.markImageByIcon(iconPath, strSourcePdfPath, strTargetPdfPath);

//        String strWaterMarkText = "我的网络股份有限公司";
//        WaterMarkUtil.waterMarkByText(strWaterMarkText, strSourcePdfPath, strTargetPdfPath,
//                0.5f, 30,
//                "宋体", 20, "gray",
//                "pdf");

        try {
            Font font = new Font("宋体", Font.PLAIN, 40);
            Field field = Color.class.getField("gray");
            Color color = (Color) field.get(null);
            createWaterMarkPng("我的网络股份有限公司", font, color, 30,
                    "d:/cvtest/wm.png");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 给pdf添加水印
     *
     * @param strIconPath      水印图片路径
     * @param strSourceImgPath 源图片路径
     * @param strTargetImgPath 目标图片路径
     */
    public static boolean waterMarkByIcon(String strIconPath, String strSourceImgPath, String strTargetImgPath) {
        return waterMarkByIcon(strIconPath, strSourceImgPath, strTargetImgPath,
                0,
                null, null, null, null);
    }

    /**
     * 给图片添加水印、可设置水印图片旋转角度
     *
     * @param strIconPath      水印图片路径
     * @param strSourcePdfPath 源图片路径
     * @param strTargetPdfPath 目标图片路径
     * @param floatAlpha       透明度，小数。例如，0.7f
     * @param intIconLocateX   水印的位置，横轴
     * @param intIconLocateY   水印的位置，纵轴
     * @param intIconWidth     水印图片的宽度
     * @param intIconHeight    水印图片的高度
     */
    public static boolean waterMarkByIcon(String strIconPath, String strSourcePdfPath, String strTargetPdfPath,
                                          float floatAlpha,
                                          Integer intIconLocateX, Integer intIconLocateY,
                                          Integer intIconWidth, Integer intIconHeight) {
        try {
            if (floatAlpha == 0) {
                floatAlpha = 0.5f;
            }

            if (intIconLocateX == null) {
                intIconLocateX = 150;
            }
            if (intIconLocateY == null) {
                intIconLocateY = 150;
            }
            if (intIconWidth == null) {
                intIconWidth = 100;
            }
            if (intIconHeight == null) {
                intIconHeight = 100;
            }

            File file = new File(strSourcePdfPath);
            PDDocument doc = Loader.loadPDF(file);

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDPage page = doc.getPage(i);
                PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);
                PDImageXObject pdImage = PDImageXObject.createFromFile(strIconPath, doc);

                PDExtendedGraphicsState pdExtGfxState = new PDExtendedGraphicsState();

                // 设置透明度
                pdExtGfxState.setNonStrokingAlphaConstant(floatAlpha);
                pdExtGfxState.setAlphaSourceFlag(true);
                pdExtGfxState.getCOSObject().setItem(COSName.BM, COSName.MULTIPLY);
                contentStream.setGraphicsStateParameters(pdExtGfxState);

                contentStream.drawImage(pdImage, intIconLocateX, intIconLocateY, intIconWidth, intIconHeight);
                contentStream.close();
                doc.save(strTargetPdfPath);
            }

            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //////////////////////////////////////////

    /**
     * 给pdf添加水印文字、可设置水印文字的旋转角度
     *
     * @param strWaterMarkText 水印文字内容
     * @param strSourcePdfPath 源图片路径和文件名
     * @param strTargetPdfPath 加水印完毕后的新图片路径和文件名
     * @param floatAlpha       透明度，小数。例如，0.7f
     * @param intDegree        文字旋转角度
     * @param strFontName      字体名称。默认“宋体”
     * @param intFontSize      字号。例如，90
     * @param strFontColor     字体颜色。例如，gray
     * @param strTargetType    目标格式。pdf/ofd
     * @return
     */
    public static boolean waterMarkByText(String strWaterMarkText, String strSourcePdfPath, String strTargetPdfPath,
                                          float floatAlpha, Integer intDegree,
                                          String strFontName, Integer intFontSize, String strFontColor,
                                          String strTargetType) {
        File fileInputPdf = new File(strSourcePdfPath);

        strTargetPdfPath = strTargetPdfPath.replaceAll("\\\\", "/");
        String strTargetPath = strTargetPdfPath.substring(0, strTargetPdfPath.lastIndexOf("/"));

        try {
            //打开pdf文件
            PDDocument pdDocument = Loader.loadPDF(fileInputPdf);
            pdDocument.setAllSecurityToBeRemoved(true);

            // 水印透明度
            if (floatAlpha == 0) {
                floatAlpha = 0.5f;
            }
            // 水印文字大小
            if (intFontSize == null) {
                intFontSize = 40;
            }

            // 水印文字颜色
            if (strFontColor == null || "".equals(strFontColor)) {
                strFontColor = "gray";
            }
            Field field = Color.class.getField(strFontColor);
            Color color = (Color) field.get(null);


            File fileWaterMarkPng = null;
            FileInputStream is = null;
            int intIconWidth = 0;
            int intIconHeight = 0;
            if ("ofd".equalsIgnoreCase(strTargetType)) {
                // 水印文字字体
                if (strFontName == null || "".equals(strFontName)) {
                    strFontName = "宋体";
                }
                Font font = new Font(strFontName, Font.PLAIN, intFontSize);

                // 根据输入的文字，生成水印png图片
                String strUUID = UUID.randomUUID().toString();
                fileWaterMarkPng = createWaterMarkPng(strWaterMarkText,
                        font, color, intDegree,
                        strTargetPath + "/" + strUUID + ".png");

                is = new FileInputStream(fileWaterMarkPng.getCanonicalFile());
                BufferedImage img = ImageIO.read(is);
                intIconWidth = img.getWidth();
                intIconHeight = img.getHeight();
            } else {
                // 水印文字字体
                if (strFontName == null || "".equals(strFontName)) {
                    strFontName = "STSONG.TTF";
                }
            }


            File file = new File(strSourcePdfPath);
            PDDocument doc = Loader.loadPDF(file);

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDPage page = doc.getPage(i);
                PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);

                // 设置透明度
                PDExtendedGraphicsState pdExtGfxState = new PDExtendedGraphicsState();
                pdExtGfxState.setNonStrokingAlphaConstant(floatAlpha);
                pdExtGfxState.setAlphaSourceFlag(true);
                pdExtGfxState.getCOSObject().setItem(COSName.BM, COSName.MULTIPLY);
                contentStream.setGraphicsStateParameters(pdExtGfxState);

                if ("pdf".equalsIgnoreCase(strTargetType)) {
                    PDFont pdfFont = PDType0Font.load(doc, new FileInputStream(System.getProperty("user.dir") + "/font/" + strFontName), true);

                    // 水印颜色
                    contentStream.setNonStrokingColor(color);

                    contentStream.beginText();

                    // 设置字体大小
                    float floatFontSize = intFontSize;
                    contentStream.setFont(pdfFont, floatFontSize);

                    // 根据水印文字大小长度计算横向坐标需要渲染几次水印
                    float h = strWaterMarkText.length() * floatFontSize;

                    for (int k = 0; k <= 10; k++) {
                        // 获取旋转实例
                        contentStream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(intDegree), k * 100, 0));

                        for (int j = 0; j < 20; j++) {
                            contentStream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(intDegree), k * h * 2, j * h));
                            contentStream.showText(strWaterMarkText);
                        }
                    }
                    contentStream.endText();
                    contentStream.restoreGraphicsState();
                    contentStream.close();

                } else {
                    PDImageXObject pdImage = PDImageXObject.createFromFile(fileWaterMarkPng.getCanonicalPath(), doc);

                    float floatPageWidth = page.getMediaBox().getWidth();
                    float floatPageHeight = page.getMediaBox().getHeight();

                    // 根据pdf每页的高度，与png图片的高度相除，得出需要生成几行
                    float floatYTurns = (floatPageHeight / intIconHeight);
                    int intYTurns = Math.round(floatYTurns);
                    // 根据pdf每页的宽度，与png图片的宽度相除，得出需要生成几次
                    float floatXTurns = (floatPageWidth / intIconWidth);
                    int intXTurns = Math.round(floatXTurns);

                    // 外层循环，根据“几行”循环
                    for (int y = 0; y < intYTurns; y++) {
                        // 计算每次生成图片Y轴坐标（图片高度累加）
                        int intIconLocateY = y * intIconHeight;

                        // 内层循环，根据“几次”循环
                        for (int x = 0; x < intXTurns; x++) {
                            // 计算每次生成图片X轴坐标（图片宽度累加）
                            int intIconLocateX = x * intIconWidth;

                            contentStream.drawImage(pdImage, intIconLocateX, intIconLocateY, intIconWidth, intIconHeight);
                        }
                    }

                    contentStream.close();
                }

                doc.save(strTargetPdfPath);
            }

            doc.close();

            if (is != null) {
                is.close();
            }

            if (fileWaterMarkPng != null && fileWaterMarkPng.exists()) {
                fileWaterMarkPng.delete();
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }


    /**
     * 根据文字生成png图片，并返回图片路径
     *
     * @param strWaterMarkText 水印文字内容
     * @param font             字体（Font对象）
     * @param colorFont        字体颜色（Color对象）
     * @param intDegree        旋转角度，整数
     * @param strWaterMarkPng  水印文件路径和文件名
     * @return 水印文件的File对象
     */
    public static File createWaterMarkPng(String strWaterMarkText, Font font, Color colorFont, Integer intDegree, String strWaterMarkPng) {
        JLabel label = new JLabel(strWaterMarkText);
        FontMetrics metrics = label.getFontMetrics(font);
        int intTextWidth = metrics.stringWidth(label.getText());//文字水印的宽


        int intTextLength = strWaterMarkText.length();
        int intOneTextWidth = intTextWidth/intTextLength;
        // 单个字斜边长
        BigDecimal bdTextLength = BigDecimal.valueOf(intOneTextWidth);
        Map<String,BigDecimal> mapTextGougu = getGouGu(bdTextLength, intDegree);
        // 对边长
        BigDecimal bdTextWidth = mapTextGougu.get("bdGou");
        int intOneTextX = bdTextWidth.intValue();
        // 临边长
        BigDecimal bdTextHeight = mapTextGougu.get("bdGu");
        int intOneTextY = bdTextHeight.intValue();


        // 斜边长
        BigDecimal bdLength = BigDecimal.valueOf(intTextWidth);
        Map<String,BigDecimal> mapGougu = getGouGu(bdLength, intDegree);
        // 对边长
        BigDecimal bdHight = mapGougu.get("bdGou");
        int intHight = bdHight.intValue() + intOneTextWidth;
        // 临边长
        BigDecimal bdWidth = mapGougu.get("bdGu");
        int intWidth = bdWidth.intValue() + intOneTextWidth;

        // 创建图片
        BufferedImage image = new BufferedImage(intWidth, intHight, BufferedImage.TYPE_INT_BGR);
        Graphics2D g = image.createGraphics();

        // 设置透明
        image = g.getDeviceConfiguration().createCompatibleImage(intWidth, intHight, Transparency.TRANSLUCENT);
        g = image.createGraphics();
        // 设置字体
        g.setFont(font);
        // 设置字体颜色
        g.setColor(colorFont);
        // 设置对线段的锯齿状边缘处理
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (null != intDegree) {
            // 设置水印旋转
            g.rotate(Math.toRadians(intDegree));
        }

        // 图片的高  除以  文字水印的宽    ——> 打印的行数(以文字水印的宽为间隔)
        int intRowsNumber = intHight / intTextWidth;
        // 图片的宽 除以 文字水印的宽   ——> 每行打印的列数(以文字水印的宽为间隔)
        int intColumnsNumber = intWidth / intTextWidth;
        // 防止图片太小而文字水印太长，所以至少打印一次
        if (intRowsNumber < 1) {
            intRowsNumber = 1;
        }
        if (intColumnsNumber < 1) {
            intColumnsNumber = 1;
        }

        for (int j = 0; j < intRowsNumber; j++) {
            for (int i = 0; i < intColumnsNumber; i++) {
                // 画出水印,并设置水印位置
                g.drawString(strWaterMarkText,
                        i * intTextWidth + j * intTextWidth + intOneTextX,
                        -i * intTextWidth + j * intTextWidth + intOneTextY);
            }
        }

        g.dispose();

        File fileWaterMarkPng = new File(strWaterMarkPng);
        try {
            // 输出png图片
            ImageIO.write(image, "png", fileWaterMarkPng);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileWaterMarkPng;
    }

    /**
     * 勾股定理，通过斜边长度、角度，计算对边（勾）和临边（股）的长度
     * @param bdXian 斜边（弦）长度
     * @param intDegree 斜边角度
     * @return Map对象，bdGou：对边（勾）长度；dbGu：临边（股）长度
     */
    private static Map<String,BigDecimal> getGouGu(BigDecimal bdXian, int intDegree){
        Map<String,BigDecimal> mapReturn = new HashMap<>();

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
