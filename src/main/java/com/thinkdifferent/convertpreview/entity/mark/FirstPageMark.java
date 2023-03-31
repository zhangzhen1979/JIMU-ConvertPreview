package com.thinkdifferent.convertpreview.entity.mark;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.utils.XHTMLToImage;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.OFDDoc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 首页水印
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/13 15:35
 */
@Data
public class FirstPageMark {

    /**
     * 水印html字符串的base64值
     */
    private String base64;
    /**
     * 水印模板路径和文件名
     */
    private String template;
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

    /**
     * 需替换参数
     */
    private Map<String, String> data;


    private static int dpi = 400;
    private static float scale = 0.5f;


    public static FirstPageMark get(Map<String, Object> mapMark) {
        FirstPageMark firstPageMark = new FirstPageMark();

        firstPageMark.setBase64(MapUtil.getStr(mapMark, "base64", null));
        firstPageMark.setTemplate(
                System.getProperty("user.dir") +
                        "/watermark/" +
                        MapUtil.getStr(mapMark, "template", "")
        );

        double dblPngWidth = MapUtil.getDouble(mapMark, "pngWidth", 1d);
        double dblPngHeight = MapUtil.getDouble(mapMark, "pngHeight", 1d);
        if(mapMark.containsKey("isCm") && MapUtil.getBool(mapMark, "isCm")){
            firstPageMark.setPngWidthMm(dblPngWidth * 10);
            firstPageMark.setPngHeightMm(dblPngHeight * 10);

            firstPageMark.setPngWidthPx((int)Math.round(dblPngWidth / 2.54 * dpi));
            firstPageMark.setPngHeightPx((int)Math.round(dblPngHeight / 2.54 * dpi));
            firstPageMark.setIsCm(true);
        }else{
            firstPageMark.setPngWidthPx((int)Math.round(dblPngWidth));
            firstPageMark.setPngHeightPx((int)Math.round(dblPngHeight));

            double dblPngWidthMm = Math.round((int)Math.round(dblPngWidth) * 100 / dpi * 2.54 * 10)/100.0;
            double dblPngHeightMm = Math.round((int)Math.round(dblPngHeight) * 100 / dpi * 2.54 * 10)/100.0;
            firstPageMark.setPngWidthMm(dblPngWidthMm);
            firstPageMark.setPngHeightMm(dblPngHeightMm);

            firstPageMark.setIsCm(false);
        }
        firstPageMark.setLocate(MapUtil.getStr(mapMark, "locate", "TC"));

        if (mapMark.containsKey("data")) {
            // 兼容非 str value
            JSONObject data = JSONObject.fromObject(mapMark.get("data"));
            Map<String, String> mapData = new HashMap<>(data.size());
            data.forEach((k, v) -> mapData.put((String) k, v.toString()));
            firstPageMark.setData(mapData);
        }

        return StringUtils.isNotBlank(firstPageMark.getBase64()) || StringUtils.isNotBlank(MapUtil.getStr(mapMark, "template")) ?
                firstPageMark : null;
    }

    /**
     * 根据传入的“首页水印对象”中的信息，生成水印PNG文件
     *
     * @return 水印图片
     * @throws IOException err
     */
    public File getMarkPng() throws IOException {
        // 生成水印png图片
        // 搞个临时文件名
        String uuid = UUID.randomUUID().toString();
        String path = System.getProperty("user.dir") + "/watermark/";
        String pngPathFile = path + uuid + ".png";

        // 判断一下，用base64直接传过来的，还是要用本地html模板
        String strHtmlPath = "";
        String strHtml = "";
        if (!StringUtils.isEmpty(this.getBase64())) {
            // 如果是base64，则转换为字符串，存成utf-8编码的html文件
            byte[] bytes = Base64.getDecoder().decode(this.getBase64());
            strHtml = new String(bytes, StandardCharsets.UTF_8);
        } else {
            // 如果是用本地html模板，则读取模板html
            strHtmlPath = this.getTemplate();
        }

        // 将html转换为png
        return XHTMLToImage.convertToImage(strHtmlPath, strHtml, pngPathFile,
                this.getPngWidthPx(), this.getPngHeightPx(), this.getData());
    }


    /**
     * PDF文件中加入首页水印
     *
     * @param pdExtGfxState
     * @param contentStream
     * @param pdDocument    PDF文档对象
     * @param page          PDF页面对象
     * @param firstPageMark 首页水印对象
     * @param modifyX
     * @throws IOException
     */
    public void mark4Pdf(PDExtendedGraphicsState pdExtGfxState,
                         PDPageContentStream contentStream,
                         PDDocument pdDocument,
                         PDPage page,
                         FirstPageMark firstPageMark,
                         float modifyX,
                         float alpha) throws IOException {
        // 生成水印png图片
        File filePng = getMarkPng();

        if (filePng != null && filePng.exists()) {
            // 获取PDF首页的尺寸大小，转换成mm
            double doublePageWidthMm = page.getMediaBox().getWidth() / 72 * 25.4;
            double doublePageHeightMm = page.getMediaBox().getHeight() / 72 * 25.4;
            // 获取水印图片高度、宽度（px、mm）
            int intPngWidthPx = firstPageMark.getPngWidthPx();
            double dblPngWidthMm = firstPageMark.getPngWidthMm();
            int intPngHeightPx = firstPageMark.getPngHeightPx();
            double dblPngHeightMm = firstPageMark.getPngHeightMm();
            PngMark pngMark = new PngMark();

            PngMarkLocal pngMarkLocal = pngMark.getPngLocateInPdf(firstPageMark.getLocate().toUpperCase(),
                    doublePageHeightMm, dblPngHeightMm,
                    doublePageWidthMm, dblPngWidthMm);

            pngMark.setWaterMarkFile(filePng.getAbsolutePath());
            pngMark.setLocateX(pngMarkLocal.getLocateX());
            pngMark.setLocateY(pngMarkLocal.getLocateY());
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
     * @param firstPageMark 首页水印对象
     * @param intPageNum    当前处理的页码
     * @throws IOException
     */
    public void mark4Ofd(OFDDoc ofdDoc,
                         ST_Box pageSize,
                         FirstPageMark firstPageMark,
                         int intPageNum,
                         float alpha) throws IOException {
        // 生成水印png图片
        File filePng = getMarkPng();

        if (filePng != null && filePng.exists()) {
            // 获取OFD页面大小
            double doublePageWidthMm = pageSize.getWidth();
            double doublePageHeightMm = pageSize.getHeight();
            // 获取水印图片高度、宽度（mm）
            int intPngWidthMm = firstPageMark.getPngWidthMm().intValue();
            int intPngHeightMm = firstPageMark.getPngHeightMm().intValue();

            PngMark pngMark = new PngMark();

            PngMarkLocal pngMarkLocal = pngMark.getPngLocateInOfd(firstPageMark.getLocate().toUpperCase(),
                    doublePageHeightMm, firstPageMark.getPngHeightMm(),
                    doublePageWidthMm, firstPageMark.getPngWidthMm());

            pngMark.setWaterMarkFile(filePng.getAbsolutePath());
            pngMark.setLocateX(pngMarkLocal.getLocateX());
            pngMark.setLocateY(pngMarkLocal.getLocateY());
            pngMark.setImageWidth(intPngWidthMm);
            pngMark.setImageHeight(intPngHeightMm);

            pngMark.mark4Ofd(ofdDoc,
                    pageSize,
                    pngMark,
                    intPageNum,
                    alpha);
            FileUtil.del(filePng);
        }

    }

}
