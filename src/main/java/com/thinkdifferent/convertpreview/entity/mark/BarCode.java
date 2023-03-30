package com.thinkdifferent.convertpreview.entity.mark;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.text.pdf.qrcode.EncodeHintType;
import com.thinkdifferent.convertpreview.utils.watermark.JpgWaterMarkUtil;
import lombok.Cleanup;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.OFDDoc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

/**
 * 条码/二维码
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/13 15:35
 */
@Data
public class BarCode {

    /**
     * 条码/二维码的编码
     */
    private String code;
    /**
     * 编码中的文字内容
     */
    private String context;
    /**
     * 是否在首页添加（否：所有页面添加）
     */
    private Boolean isFirstPage;

    /**
     * 水印图片宽度
     */
    private int pngWidth;
    /**
     * 水印图片高度
     */
    private int pngHeight;
    /**
     * 图片尺寸单位，是否为“厘米”
     */
    private Boolean isCm;
    /**
     * 水印位置
     */
    private String locate;

    private static int dpi = 400;
    private static float scale = 0.5f;

    public static BarCode get(Map<String, Object> mapMark) {
        BarCode barCode = new BarCode();

        barCode.setCode(MapUtil.getStr(mapMark, "code", "QR_CODE"));
        barCode.setContext(MapUtil.getStr(mapMark, "context", ""));
        barCode.setIsFirstPage(MapUtil.getBool(mapMark, "isFirstPage", true));

        double dblPngWidth = MapUtil.getDouble(mapMark, "pngWidth", 1d);
        double dblPngHeight = MapUtil.getDouble(mapMark, "pngHeight", 1d);
        int intPngWidth = 0;
        int intPngHeight = 0;
        if(mapMark.containsKey("isCm") && MapUtil.getBool(mapMark, "isCm")){
            intPngWidth = (int)Math.round(dblPngWidth / 2.54 * dpi);
            intPngHeight = (int)Math.round(dblPngHeight / 2.54 * dpi);
        }
        barCode.setPngWidth(intPngWidth);
        barCode.setPngHeight(intPngHeight);
        barCode.setIsCm(true);
        barCode.setLocate(MapUtil.getStr(mapMark, "locate", "TL"));

        return barCode;
    }

    /**
     * 根据传入的信息，生成二维码/条码水印PNG文件
     *
     * @return 水印图片
     * @throws IOException err
     */
    public File getMarkPng() throws IOException, WriterException {
        // 生成二维码/条码png图片
        // 搞个临时文件名
        String uuid = UUID.randomUUID().toString();
        String path = System.getProperty("user.dir") + "/watermark/";
        String pngPathFile = path + uuid + ".png";

        if (!StringUtils.isEmpty(this.getCode()) &&
                !StringUtils.isEmpty(this.getContext()) ) {
            BufferedImage bufferedImage = createBarCodeBufferdImage(this.getCode(), this.getContext(),
                    this.getPngWidth(), this.getPngHeight());
            File filePng = new File(pngPathFile);
            boolean blnFlag = ImageIO.write(bufferedImage, "png", filePng);
            if(blnFlag){
                bufferedImage = null;
            }

            return filePng;
        }else{
            return null;
        }
    }


    /**
     * 生成二维码/条码
     *
     * @param code 编码格式
     * @param contents 二维码的内容
     * @param width 二维码图片宽度
     * @param height 二维码图片高度
     */
    public BufferedImage createBarCodeBufferdImage(String code, String contents,
                                                   int width, int height) throws WriterException {
        Hashtable hints= new Hashtable();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                contents, BarcodeFormat.valueOf(code), width, height, hints);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        return image;
    }



    /**
     * PDF文件中加入水印
     *
     * @param pdExtGfxState
     * @param contentStream
     * @param pdDocument    PDF文档对象
     * @param page          PDF页面对象
     * @param barCode       二维码/条码对象
     * @param modifyX
     * @throws IOException
     */
    public void mark4Pdf(PDExtendedGraphicsState pdExtGfxState,
                         PDPageContentStream contentStream,
                         PDDocument pdDocument,
                         PDPage page,
                         BarCode barCode,
                         float modifyX,
                         float alpha) throws IOException, WriterException {
        // 获取归档章png图片
        File filePng = getMarkPng();

        if (filePng != null && filePng.exists()) {
            // 获取PDF首页的尺寸大小，转换成mm
            double doublePageWidthMm = page.getMediaBox().getWidth() / 72 * 25.4;
            double doublePageHeightMm = page.getMediaBox().getHeight() / 72 * 25.4;
            // 获取水印图片高度、宽度（px）
            int intPngWidthPx = barCode.getPngWidth();
            double dblPngWidthMm = Math.round(intPngWidthPx * 100 / dpi * 2.54 * 10)/100.0;
            int intPngHeightPx = barCode.getPngHeight();
            double dblPngHeightMm = Math.round(intPngHeightPx * 100 / dpi * 2.54 * 10)/100.0;

            float floatIconLocateX = 0f;
            float floatIconLocateY = 0f;

            switch (barCode.getLocate().toUpperCase()){
                case "TL":
                    floatIconLocateX = 3;
                    floatIconLocateY = (float)((doublePageHeightMm - dblPngHeightMm ) * scale + 3);
                    break;
                case "TM":
                    floatIconLocateX = (float)((doublePageWidthMm - dblPngWidthMm ) * scale * 0.5 );
                    floatIconLocateY = (float)((doublePageHeightMm - dblPngHeightMm ) * scale + 3);
                    break;
                case "TR":
                    floatIconLocateX = (float)((doublePageWidthMm - dblPngWidthMm ) * scale + 3);
                    floatIconLocateY = (float)((doublePageHeightMm - dblPngHeightMm ) * scale + 3);
                    break;
                case "CL":
                    floatIconLocateX = 3;
                    floatIconLocateY = (float)((doublePageHeightMm - dblPngHeightMm ) * scale * 0.5);
                    break;
                case "C":
                    floatIconLocateX = (float)((doublePageWidthMm - dblPngWidthMm ) * scale * 0.5 );
                    floatIconLocateY = (float)((doublePageHeightMm - dblPngHeightMm ) * scale * 0.5);
                    break;
                case "CR":
                    floatIconLocateX = (float)((doublePageWidthMm - dblPngWidthMm) * scale + 3);
                    floatIconLocateY = (float)((doublePageHeightMm - dblPngHeightMm ) * scale * 0.5);
                    break;
                case "BL":
                    floatIconLocateX = 3;
                    floatIconLocateY = 3;
                    break;
                case "BM":
                    floatIconLocateX = (float)((doublePageWidthMm - dblPngWidthMm ) * scale * 0.5 );
                    floatIconLocateY = 3;
                    break;
                case "BR":
                    floatIconLocateX = (float)((doublePageWidthMm - dblPngWidthMm) * scale + 3);
                    floatIconLocateY = 3;
            }

            PngMark pngMark = new PngMark();
            pngMark.setWaterMarkFile(filePng.getAbsolutePath());
            pngMark.setLocateX(floatIconLocateX);
            pngMark.setLocateY(floatIconLocateY);
            pngMark.setImageWidth(intPngWidthPx);
            pngMark.setImageHeight(intPngHeightPx);

            pngMark.mark4Pdf(pdExtGfxState,
                    contentStream,
                    pdDocument,
                    page,
                    pngMark,
                    modifyX,
                    alpha);
            FileUtil.del(filePng);
        }

    }

    /**
     * OFD文件中加入首页水印
     *
     * @param ofdDoc        OFD页面对象
     * @param pageSize      页面尺寸对象
     * @param barCode       二维码/条码对象
     * @param intPageNum    当前处理的页码
     * @throws IOException
     */
    public void mark4Ofd(OFDDoc ofdDoc,
                         ST_Box pageSize,
                         BarCode barCode,
                         int intPageNum,
                         float alpha) throws IOException, WriterException {
        // 生成png图片
        File filePng = getMarkPng();

        if (filePng != null && filePng.exists()) {
            // 获取OFD页面大小
            double dblPageWidth = pageSize.getWidth();
            double dblPageHeight = pageSize.getHeight();
            // 获取水印图片高度、宽度
            int intPngWidth = barCode.getPngWidth();
            int intPngHeight = barCode.getPngHeight();

            float floatIconLocateX = (float) (dblPageWidth - intPngWidth / 12) / 2;
            float floatIconLocateY = (float) (dblPageHeight - intPngHeight / 12 - 5);

            switch (barCode.getLocate().toUpperCase()){
                case "TR":
                    floatIconLocateX = (float) (dblPageWidth - intPngWidth / 12 - 5);
                    break;
                case "TL":
                    floatIconLocateX = 5;
                    break;
                case "CR":
                    floatIconLocateX = (float) (dblPageWidth - intPngWidth / 12 - 5);
                    floatIconLocateY = (float) (dblPageHeight - intPngHeight / 12) / 2;
                    break;
                case "C":
                    floatIconLocateX = (float) (dblPageWidth - intPngWidth / 12) / 2;
                    floatIconLocateY = (float) (dblPageHeight - intPngHeight / 12) / 2;
                    break;
                case "CL":
                    floatIconLocateX = 5;
                    floatIconLocateY = (float) (dblPageHeight - intPngHeight / 12) / 2;
                    break;
                case "BR":
                    floatIconLocateX = (float) (dblPageWidth - intPngWidth / 12 - 5);
                    floatIconLocateY = 5;
                    break;
                case "BM":
                    floatIconLocateX = (float) (dblPageWidth - intPngWidth / 12) / 2;
                    floatIconLocateY = 5;
                    break;
                case "BL":
                    floatIconLocateX = 5;
                    floatIconLocateY = 5;
            }

            // 输入的是像素，需要转换为毫米。300dpi，1mm=12px
            PngMark pngMark = new PngMark();
            pngMark.setWaterMarkFile(filePng.getAbsolutePath());
            pngMark.setLocateX(floatIconLocateX);
            pngMark.setLocateY(floatIconLocateY);
            pngMark.setImageWidth(intPngWidth / 12);
            pngMark.setImageHeight(intPngHeight / 12);

            pngMark.mark4Ofd(ofdDoc,
                    pageSize,
                    pngMark,
                    intPageNum,
                    alpha);
            FileUtil.del(filePng);
        }

    }

    /**
     * 为图片添加归档章水印
     *
     * @param strInputJpg   输入的JPG文件
     * @param strOutputJpg  输出的JPG文件
     * @param barCode       二维码/条码对象
     */
    public void mark4Jpg(String strInputJpg, String strOutputJpg,
                         BarCode barCode) throws Exception {
        File fileSourceImg = new File(strInputJpg);
        @Cleanup FileInputStream input = new FileInputStream(fileSourceImg);
        BufferedImage buffSourceImg = ImageIO.read(input);
        BufferedImage buffImg = new BufferedImage(buffSourceImg.getWidth(), buffSourceImg.getHeight(), BufferedImage.TYPE_INT_RGB);
        // 获取图片的大小
        int intImageWidth = buffImg.getWidth();

        // 获取水印图片高度、宽度
        int intPngWidth = barCode.getPngWidth();
        int intPngHeight = barCode.getPngHeight();

        // 计算水印图片在右上角的坐标
        int intIconLocateX = (intImageWidth - intPngWidth) / 2;
        int intIconLocateY = 100;

        File filePng = getMarkPng();

        if (filePng.exists()) {
            JpgWaterMarkUtil.markImageByImage(filePng.getAbsolutePath(),
                    strInputJpg, strOutputJpg,
                    0,
                    intIconLocateX, intIconLocateY,
                    intPngWidth, intPngHeight);
            FileUtil.del(filePng);
        }

    }

}
