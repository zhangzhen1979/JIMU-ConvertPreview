package com.thinkdifferent.convertpreview.entity.mark;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
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
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

/**
 * 条码/二维码
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
     * 水印图片像素宽度（px）
     */
    private int pngWidthPx;
    /**
     * 水印图片像素高度（px）
     */
    private int pngHeightPx;
    /**
     * 水印图片毫米宽度（mm）
     */
    private Double pngWidthMm;
    /**
     * 水印图片毫米高度（mm）
     */
    private Double pngHeightMm;
    /**
     * 图片尺寸单位，是否为“厘米”
     */
    private Boolean isCm;
    /**
     * 水印位置
     */
    private String locate;

    private static int dpi = 400;

    public static BarCode get(Map<String, Object> mapMark) {
        BarCode barCode = new BarCode();

        barCode.setCode(MapUtil.getStr(mapMark, "code", "QR_CODE"));
        barCode.setContext(MapUtil.getStr(mapMark, "context", ""));
        barCode.setIsFirstPage(MapUtil.getBool(mapMark, "isFirstPage", true));

        double dblPngWidth = MapUtil.getDouble(mapMark, "pngWidth", 1d);
        double dblPngHeight = MapUtil.getDouble(mapMark, "pngHeight", 1d);
        if(mapMark.containsKey("isCm") && MapUtil.getBool(mapMark, "isCm")){
            barCode.setPngWidthMm(dblPngWidth * 10);
            barCode.setPngHeightMm(dblPngHeight * 10);

            barCode.setPngWidthPx((int)Math.round(dblPngWidth / 2.54 * dpi));
            barCode.setPngHeightPx((int)Math.round(dblPngHeight / 2.54 * dpi));
            barCode.setIsCm(true);
        }else{
            barCode.setPngWidthPx((int)Math.round(dblPngWidth));
            barCode.setPngHeightPx((int)Math.round(dblPngHeight));

            double dblPngWidthMm = Math.round((int)Math.round(dblPngWidth) * 100 / dpi * 2.54 * 10)/100.0;
            double dblPngHeightMm = Math.round((int)Math.round(dblPngHeight) * 100 / dpi * 2.54 * 10)/100.0;
            barCode.setPngWidthMm(dblPngWidthMm);
            barCode.setPngHeightMm(dblPngHeightMm);

            barCode.setIsCm(false);
        }
        barCode.setLocate(MapUtil.getStr(mapMark, "locate", "TL"));

        return barCode;
    }

    /**
     * 根据传入的信息，生成二维码/条码水印PNG文件
     *
     * @return 水印图片
     * @throws IOException err
     */
    public File getBarcodePng() throws IOException, WriterException {
        // 生成二维码/条码png图片
        // 搞个临时文件名
        String uuid = UUID.randomUUID().toString();
        String path = System.getProperty("user.dir") + "/watermark/";
        String pngPathFile = path + uuid + ".png";

        if (!StringUtils.isEmpty(this.getCode()) &&
                !StringUtils.isEmpty(this.getContext()) ) {
            BufferedImage bufferedImage = createBarCodeBufferdImage(this.getCode(), this.getContext(),
                    this.getPngWidthPx(), this.getPngWidthPx());
            File filePng = new File(pngPathFile);
            ImageIO.write(bufferedImage, "png", filePng);

            if (bufferedImage != null) {
                bufferedImage.getGraphics().dispose();
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
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        int c = image.getRGB(3, 3);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : c & 0x00ffffff);
            }
        }

        return image;
    }



    /**
     * PDF文件中加入二维码
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
        // 获取二维码png图片
        File filePng = getBarcodePng();

        if (filePng != null && filePng.exists()) {
            PngMark pngMark = new PngMark();
            pngMark.setWaterMarkFile(filePng.getAbsolutePath());
            pngMark.setImageWidth(barCode.getPngWidthPx());
            pngMark.setImageHeight(barCode.getPngWidthPx());

            pngMark.mark4Pdf(pdExtGfxState,
                    contentStream,
                    pdDocument,
                    page,
                    pngMark,
                    barCode.getLocate(),
                    alpha);
        }
        FileUtil.del(filePng);

    }

    /**
     * OFD文件中加入二维码
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
        File filePng = getBarcodePng();

        if (filePng != null && filePng.exists()) {
            PngMark pngMark = new PngMark();
            pngMark.setWaterMarkFile(filePng.getAbsolutePath());
            pngMark.setImageWidth(barCode.getPngWidthMm().intValue());
            pngMark.setImageHeight(barCode.getPngHeightMm().intValue());

            pngMark.mark4Ofd(ofdDoc,
                    pageSize,
                    pngMark,
                    barCode.getLocate(),
                    intPageNum,
                    alpha);
        }
        FileUtil.del(filePng);

    }

}
