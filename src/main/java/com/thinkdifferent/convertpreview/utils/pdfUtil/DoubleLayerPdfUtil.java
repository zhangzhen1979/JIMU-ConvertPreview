package com.thinkdifferent.convertpreview.utils.pdfUtil;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.entity.Context;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.util.Assert;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 双层PDF工具类
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/12 15:45
 */
@Log4j2
public class DoubleLayerPdfUtil {
    private static File ttfFile;

    private DoubleLayerPdfUtil() {
    }

    private static File getTtfFile() {
        if (ttfFile == null) {
            String fontFilePath = ConvertDocConfigBase.waterMarkFont;
            Assert.isTrue(new File(fontFilePath).exists(), "字体文件【" + fontFilePath + "】不存在");
            ttfFile = new File(fontFilePath);
        }
        return ttfFile;
    }

    /**
     * 双层PDF
     *
     * @param basePdfFile 原始PDF
     * @param contexts    OCR识别结果
     */
    public static void addText(File basePdfFile, List<Context> contexts) throws IOException {
        String path = basePdfFile.getParent() + "/";

        File tempFile = new File(path + UUID.randomUUID() + basePdfFile.getName().substring(basePdfFile.getName().lastIndexOf(".")));
        // FileUtils.moveFile();
        @Cleanup PDDocument document = Loader.loadPDF(basePdfFile);
        document.setAllSecurityToBeRemoved(true);

        for (Context context : contexts) {
            // PDF 写入文字
            PDPage page = document.getPage(context.getPageIndex());

            PDRectangle mediaBox = page.getMediaBox();

            // 使用配置文件字体
            PDFont pdfFont = PDType0Font.load(document, getTtfFile());

            @Cleanup PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);

            // 设置透明度
            PDExtendedGraphicsState pdExtGfxState = new PDExtendedGraphicsState();
            pdExtGfxState.setNonStrokingAlphaConstant(0F);
            pdExtGfxState.setAlphaSourceFlag(true);
            //pdExtGfxState.getCOSObject().setItem(COSName.BM, COSName.MULTIPLY);
            contentStream.setGraphicsStateParameters(pdExtGfxState);

            // 水印颜色
            // contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2F);
            contentStream.setNonStrokingColor(Color.RED);

            contentStream.beginText();

            for (Context.Word word : context.getWords()) {
                // 设置字体大小
                contentStream.setFont(pdfFont, fontSize(word.getText(), word.getRect().getWidth()));
                // OCR结果是从左上算起点， PDFBOX 从左下算起点
                contentStream.setTextMatrix(Matrix.getTranslateInstance((float) word.getRect().getX()
                        // 页面高度 - 纵坐标 - 文字高度的一半
                        , (float) (mediaBox.getHeight() - word.getRect().getY() - word.getRect().getHeight())));
                contentStream.showText(word.getText());
            }
            contentStream.endText();
            contentStream.restoreGraphicsState();
        }
        document.save(tempFile);
        FileUtil.rename(tempFile, basePdfFile.getName(), true);

    }

    private static float fontSize(String text, double rectWidth) {
        int allLength = text.length();
        String strEN = ReUtil.replaceAll(text, RegexPool.CHINESES, "");
        int intChineseNumber = allLength - strEN.length();
        // 0.8
        int intMin = CharSequenceUtil.count(strEN, 'w') + CharSequenceUtil.count(strEN, 'm');
        // 1
        int intMax = CharSequenceUtil.count(strEN, 'W') + CharSequenceUtil.count(strEN, 'M');
        // 0.25
        int intMinIJ = CharSequenceUtil.count(strEN, 'i') + CharSequenceUtil.count(strEN, 'j')
                + CharSequenceUtil.count(strEN, '|')
                + CharSequenceUtil.count(strEN, '.') + CharSequenceUtil.count(strEN, ',');
        // 计算汉字长度
        double doubleEnLength = intChineseNumber +
                intMin * 0.8 + intMax + intMinIJ * 0.25 + (strEN.length() - intMin - intMax - intMinIJ) * 0.5;

        return (float) (rectWidth / doubleEnLength);
    }
}
