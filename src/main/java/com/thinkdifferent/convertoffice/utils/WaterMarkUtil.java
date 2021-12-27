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
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.edit.Annotation;
import org.ofdrw.layout.element.canvas.FontSetting;
import org.ofdrw.reader.OFDReader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class WaterMarkUtil {

    /**
     * @param args
     */
    public static void main(String[] args) {

        // 添加水印
//        String iconPath = "d:/watermark.png";

//        String strSourcePdfPath = "d:/cvtest/1.pdf";
//        String strTargetPdfPath = "d:/cvtest/1-water.pdf";
//        WaterMarkUtil.waterMarkByIcon4Pdf(iconPath, strSourcePdfPath, strTargetPdfPath,
//                1, 45,
//                200,100,
//                150,150);

//        String strSourcePdfPath = "d:/cvtest/test.ofd";
//        String strTargetPdfPath = "d:/cvtest/test-water.ofd";
//        WaterMarkUtil.waterMarkByIcon4Ofd(iconPath, strSourcePdfPath, strTargetPdfPath,
//                1, 0,
//                100,50,
//                50,50);


//        try {
//            String strSourcePdfPath = "d:/cvtest/1.pdf";
//            String strTargetPdfPath = "d:/cvtest/1-water.pdf";
//            String strWaterMarkText = "我的网络股份有限公司";
//            Field field = Color.class.getField("gray");
//            Color color = (Color) field.get(null);
//
//            WaterMarkUtil.waterMarkByText4Pdf(strSourcePdfPath, strTargetPdfPath,
//                    strWaterMarkText,
//                    0.5f, 30,
//                    "STSONG.TTF", 20, color);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        try {
            String strSourcePdfPath = "d:/cvtest/1.ofd";
            String strTargetPdfPath = "d:/cvtest/1-water.ofd";
            String strWaterMarkText = "内部文件";
            Field field = Color.class.getField("gray");
            Color color = (Color) field.get(null);

            WaterMarkUtil.waterMarkByText4Ofd(strSourcePdfPath, strTargetPdfPath,
                    strWaterMarkText,
                    10, 0.5,
                    color, 45);

        } catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            Font font = new Font("宋体", Font.PLAIN, 40);
//            Field field = Color.class.getField("gray");
//            Color color = (Color) field.get(null);
//            createWaterMarkPng("我的网络股份有限公司", font, color, 30,
//                    "d:/cvtest/wm.png");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    /**
     * 给pdf添加水印
     *
     * @param strIconPath      水印图片路径
     * @param strSourceImgPath 源图片路径
     * @param strTargetImgPath 目标图片路径
     */
    public static boolean waterMarkByIcon4Pdf(String strIconPath, String strSourceImgPath, String strTargetImgPath) {
        return waterMarkByIcon4Pdf(strIconPath, strSourceImgPath, strTargetImgPath,
                0, 0,
                null, null, null, null);
    }

    /**
     * 给PDF添加图片水印、可设置水印图片旋转角度
     *
     * @param strIconPath      水印图片路径
     * @param strSourcePdfPath 源图片路径
     * @param strTargetPdfPath 目标图片路径
     * @param floatAlpha       透明度，小数。例如，0.7f
     * @param intDegree        旋转角度
     * @param intIconLocateX   水印的位置，横轴
     * @param intIconLocateY   水印的位置，纵轴
     * @param intIconWidth     水印图片的宽度
     * @param intIconHeight    水印图片的高度
     */
    public static boolean waterMarkByIcon4Pdf(String strIconPath, String strSourcePdfPath, String strTargetPdfPath,
                                              float floatAlpha, int intDegree,
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

                contentStream.transform(Matrix.getRotateInstance(Math.toRadians(intDegree), intIconLocateX, intIconLocateY));

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


    /**
     * 给PDF添加水印文字、可设置水印文字的旋转角度
     *
     * @param strSourcePdfPath 源图片路径和文件名
     * @param strTargetPdfPath 加水印完毕后的新图片路径和文件名
     * @param strWaterMarkText 水印文字内容
     * @param floatAlpha       透明度，小数。例如，0.7f
     * @param intDegree        文字旋转角度
     * @param strFontName      字体名称。默认“宋体”
     * @param intFontSize      字号。例如，90
     * @param color            字体颜色。
     * @return
     */
    public static boolean waterMarkByText4Pdf(String strSourcePdfPath, String strTargetPdfPath,
                                              String strWaterMarkText,
                                              float floatAlpha, Integer intDegree,
                                              String strFontName, Integer intFontSize, Color color) {
        File fileInputPdf = new File(strSourcePdfPath);

        strTargetPdfPath = strTargetPdfPath.replaceAll("\\\\", "/");

        try {
            //打开pdf文件
            PDDocument pdDocument = Loader.loadPDF(fileInputPdf);
            pdDocument.setAllSecurityToBeRemoved(true);

            // 水印文字字体
            if (strFontName == null || "".equals(strFontName)) {
                strFontName = "STSONG.TTF";
            }
            // 水印透明度
            if (floatAlpha == 0) {
                floatAlpha = 0.5f;
            }
            // 水印文字大小
            if (intFontSize == null) {
                intFontSize = 40;
            }
            float floatFontSize = intFontSize;

            File file = new File(strSourcePdfPath);
            PDDocument doc = Loader.loadPDF(file);
            PDFont pdfFont = PDType0Font.load(doc, new FileInputStream(System.getProperty("user.dir") + "/font/" + strFontName), true);

            // 设置透明度
            PDExtendedGraphicsState pdExtGfxState = new PDExtendedGraphicsState();
            pdExtGfxState.setNonStrokingAlphaConstant(floatAlpha);
            pdExtGfxState.setAlphaSourceFlag(true);
            pdExtGfxState.getCOSObject().setItem(COSName.BM, COSName.MULTIPLY);


            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDPage page = doc.getPage(i);
                PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);

                contentStream.setGraphicsStateParameters(pdExtGfxState);

                // 水印颜色
                contentStream.setNonStrokingColor(color);

                contentStream.beginText();
                // 设置字体大小
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

            }
            doc.save(strTargetPdfPath);
            doc.close();
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
        int intOneTextWidth = intTextWidth / intTextLength;
        // 单个字斜边长
        BigDecimal bdTextLength = BigDecimal.valueOf(intOneTextWidth);
        Map<String, BigDecimal> mapTextGougu = getGouGu(bdTextLength, intDegree);
        // 对边长
        BigDecimal bdTextWidth = mapTextGougu.get("bdGou");
        int intOneTextX = bdTextWidth.intValue();
        // 临边长
        BigDecimal bdTextHeight = mapTextGougu.get("bdGu");
        int intOneTextY = bdTextHeight.intValue();


        // 斜边长
        BigDecimal bdLength = BigDecimal.valueOf(intTextWidth);
        Map<String, BigDecimal> mapGougu = getGouGu(bdLength, intDegree);
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
     *
     * @param bdXian    斜边（弦）长度
     * @param intDegree 斜边角度
     * @return Map对象，bdGou：对边（勾）长度；dbGu：临边（股）长度
     */
    private static Map<String, BigDecimal> getGouGu(BigDecimal bdXian, int intDegree) {
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
     * 为OFD添加图片水印
     *
     * @param strWaterMarkIcon 水印图片文件路径和文件名
     * @param strInputFile     输入的OFD文件
     * @param strOutputFile    输出的OFD文件（加水印）
     * @param dblAlpha         透明度
     * @param intDegree        旋转角度
     * @param dblIconLocateX   水印X轴位置
     * @param dblIconLocateY   水印Y轴位置
     * @param dblIconWidth     水印宽度
     * @param dblIconHeight    水印高度
     * @return
     */
    public static boolean waterMarkByIcon4Ofd(String strWaterMarkIcon, String strInputFile, String strOutputFile,
                                              double dblAlpha, Integer intDegree,
                                              double dblIconLocateX, double dblIconLocateY,
                                              double dblIconWidth, double dblIconHeight) {

        Path pathInput = Paths.get(strInputFile);
        Path pathOutput = Paths.get(strOutputFile);
        Path pathIcon = Paths.get(strWaterMarkIcon);

        try (OFDReader reader = new OFDReader(pathInput);
             OFDDoc ofdDoc = new OFDDoc(reader, pathOutput)) {

            int intPages = reader.getNumberOfPages();

            Double dblWidth = ofdDoc.getPageLayout().getWidth();
            Double dblHeight = ofdDoc.getPageLayout().getHeight();

            Annotation annotation = new Annotation(new ST_Box(0d, 0d, dblWidth, dblHeight), AnnotType.Watermark, ctx -> {

                ctx.setGlobalAlpha(dblAlpha);

                ctx.save();
                ctx.rotate(-intDegree);
                ctx.drawImage(pathIcon,
                        dblIconLocateX, dblIconLocateY,
                        dblIconWidth, dblIconHeight);


                ctx.restore();
            });

            for (int i = 1; i <= intPages; i++) {
                ofdDoc.addAnnotation(i, annotation);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 为OFD文件添加文字水印
     *
     * @param strInputFile     输入文件路径和文件名
     * @param strOutputFile    输出文件路径和文件名（完成加水印）
     * @param strWaterMarkText 水印文字内容
     * @param floatFontSize    字号
     * @param dblAlpha         透明度
     * @param colorFont        文字颜色
     * @param intDegree        旋转角度
     * @throws IOException
     */
    public static boolean waterMarkByText4Ofd(String strInputFile, String strOutputFile,
                                              String strWaterMarkText,
                                              float floatFontSize, double dblAlpha, Color colorFont,
                                              Integer intDegree) {

        Path pathInput = Paths.get(strInputFile);
        Path pathOutput = Paths.get(strOutputFile);

        float h = strWaterMarkText.length() * floatFontSize;

        try (OFDReader reader = new OFDReader(pathInput);
             OFDDoc ofdDoc = new OFDDoc(reader, pathOutput)) {

            int intPages = reader.getNumberOfPages();

            double dblWidth = ofdDoc.getPageLayout().getWidth();
            double dblHeight = ofdDoc.getPageLayout().getHeight();
            final double dblBorderLength = Math.max(dblHeight, dblWidth);

            Annotation annotation = new Annotation(new ST_Box(0d, 0d, dblBorderLength, dblBorderLength), AnnotType.Watermark, ctx -> {
                FontSetting setting = new FontSetting(floatFontSize, FontName.SimSun.font());

                ctx.setFillColor(colorFont.getRed(), colorFont.getGreen(), colorFont.getBlue())
                        .setFont(setting)
                        .setGlobalAlpha(dblAlpha);

                //对ofd页面填充4行10列的水印，并顺时针旋转45°
                for (int i = 0; i < dblBorderLength/h ; i++) {// 每行打印次数
                    for (int j = 0; j < dblBorderLength/20; j++) { // 打印的行数
                        ctx.save();
                        ctx.rotate(-intDegree);
                        ctx.translate(40 * i, j * 20); // 控制x、y轴间距
                        ctx.fillText(strWaterMarkText,  i * h - dblBorderLength , j * h -dblBorderLength);
                        ctx.restore();
                    }
                }
            });

            for (int i = 1; i <= intPages; i++) {
                ofdDoc.addAnnotation(i, annotation);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
