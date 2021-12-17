package com.thinkdifferent.convertoffice.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

public class WaterMarkUtil {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String strSourcePdfPath = "d:/cvtest/1-online.pdf";
        String strTargetPdfPath = "d:/cvtest/1-water.pdf";
//        // 添加水印
////        String iconPath = "d:/watermark.png";
////        WaterMarkUtil.markImageByIcon(iconPath, strSourcePdfPath, strTargetPdfPath);
//
        String strWaterMarkText = "我的网络股份有限公司";
        WaterMarkUtil.waterMarkByText( strWaterMarkText, strSourcePdfPath, strTargetPdfPath,
                0.5f,45,
                "宋体",20,"gray");

//        try {
//            Font font = new Font("宋体", Font.PLAIN, 40);
//            Field field = Color.class.getField("gray");
//            Color color = (Color) field.get(null);
//            createWaterMarkPng("我的网络股份有限公司", font, color, 30,
//                    "d:/cvtest/wm.png");
//
//        } catch (Exception e) {
//
//        }

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
     * @param strWaterMarkText  水印文字内容
     * @param strSourcePdfPath  源图片路径和文件名
     * @param strTargetPdfPath  加水印完毕后的新图片路径和文件名
     * @param floatAlpha        透明度，小数。例如，0.7f
     * @param intDegree         文字旋转角度
     * @param strFontName       字体名称。默认“宋体”
     * @param intFontSize       字号。例如，90
     * @param strFontColor      字体颜色。例如，gray
     * @return
     */
    public static boolean waterMarkByText(String strWaterMarkText, String strSourcePdfPath, String strTargetPdfPath,
                                          float floatAlpha, Integer intDegree,
                                          String strFontName, Integer intFontSize, String strFontColor) {
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

            // 水印文字字体
            if (strFontName == null || "".equals(strFontName)) {
                strFontName = "宋体";
            }
            Font font = new Font(strFontName, Font.PLAIN, intFontSize);

            // 水印文字颜色
            if (strFontColor == null || "".equals(strFontColor)) {
                strFontColor = "gray";
            }
            Field field = Color.class.getField(strFontColor);
            Color color = (Color) field.get(null);

            // 根据输入的文字，生成水印png图片
            String strUUID = UUID.randomUUID().toString();
            File fileWaterMarkPng = createWaterMarkPng(strWaterMarkText,
                    font, color, intDegree,
                    strTargetPath + "/" + strUUID + ".png");


            FileInputStream is = new FileInputStream(fileWaterMarkPng.getCanonicalFile());
            BufferedImage img = ImageIO.read(is);
            int intIconWidth = img.getWidth();
            int intIconHeight = img.getHeight();

            File file = new File(strSourcePdfPath);
            PDDocument doc = Loader.loadPDF(file);
            PDImageXObject pdImage = PDImageXObject.createFromFile(fileWaterMarkPng.getCanonicalPath(), doc);

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDPage page = doc.getPage(i);
                float floatPageWidth = page.getMediaBox().getWidth();
                float floatPageHeight = page.getMediaBox().getHeight();

                PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);

                PDExtendedGraphicsState pdExtGfxState = new PDExtendedGraphicsState();
                // 设置透明度
                pdExtGfxState.setNonStrokingAlphaConstant(floatAlpha);
                pdExtGfxState.setAlphaSourceFlag(true);
                pdExtGfxState.getCOSObject().setItem(COSName.BM, COSName.MULTIPLY);
                contentStream.setGraphicsStateParameters(pdExtGfxState);


                // 根据pdf每页的高度，与png图片的高度相除，得出需要生成几行
                float floatYTurns = (floatPageHeight/intIconHeight);
                int intYTurns = Math.round(floatYTurns);
                // 根据pdf每页的宽度，与png图片的宽度相除，得出需要生成几次
                float floatXTurns = (floatPageWidth/intIconWidth);
                int intXTurns = Math.round(floatXTurns);

                // 外层循环，根据“几行”循环
                for(int y=0; y<intYTurns; y++){
                    // 计算每次生成图片Y轴坐标（图片高度累加）
                    int intIconLocateY = y * intIconHeight;

                    // 内层循环，根据“几次”循环
                    for(int x=0; x<intXTurns; x++){
                        // 计算每次生成图片X轴坐标（图片宽度累加）
                        int intIconLocateX = x * intIconWidth;

                        contentStream.drawImage(pdImage, intIconLocateX, intIconLocateY, intIconWidth, intIconHeight);
                    }
                }

                contentStream.close();
                doc.save(strTargetPdfPath);
            }

            doc.close();

            is.close();
            fileWaterMarkPng.delete();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }


    /**
     * 根据文字生成png图片，并返回图片路径
     * @param strWaterMarkText 水印文字内容
     * @param font 字体（Font对象）
     * @param colorFont 字体颜色（Color对象）
     * @param intDegree 旋转角度，整数
     * @param strWaterMarkPng 水印文件路径和文件名
     * @return 水印文件的File对象
     */
    public static File createWaterMarkPng(String strWaterMarkText, Font font, Color colorFont, Integer intDegree, String strWaterMarkPng) {
        JLabel label = new JLabel(strWaterMarkText);
        FontMetrics metrics = label.getFontMetrics(font);
        int intTextWidth = metrics.stringWidth(label.getText());//文字水印的宽

        //斜边长
        BigDecimal bdLength = BigDecimal.valueOf(intTextWidth);
        //转换为弧度制
        double dblRadians = Math.toRadians(intDegree);
        //正弦值
        BigDecimal gdSin = BigDecimal.valueOf(Math.sin(dblRadians));
        //四舍五入保留2位小数
        gdSin = gdSin.setScale(2, BigDecimal.ROUND_HALF_UP);
        //a边长
        BigDecimal bdHight = bdLength.multiply(gdSin);
        int intHight = bdHight.intValue() + 80;

        //余弦值
        BigDecimal bdCos = BigDecimal.valueOf(Math.cos(dblRadians));
        //四舍五入保留2位小数
        bdCos = bdCos.setScale(2, BigDecimal.ROUND_HALF_UP);
        //b边长
        BigDecimal bdWidth = bdLength.multiply(bdCos);
        int intWidth = bdWidth.intValue() + 50;



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
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

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
                        i * intTextWidth + j * intTextWidth + 50,
                        -i * intTextWidth + j * intTextWidth + 50);
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


}
