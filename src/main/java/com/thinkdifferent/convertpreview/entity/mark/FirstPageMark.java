package com.thinkdifferent.convertpreview.entity.mark;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.utils.WaterMarkUtil;
import com.thinkdifferent.convertpreview.utils.XHTMLToImage;
import lombok.SneakyThrows;
import net.sf.json.JSONObject;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 首页水印（归档章）
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/13 15:35
 */
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
     * 水印图片宽度
     */
    private int pngWidth;
    /**
     * 水印图片高度
     */
    private int pngHeight;
    /**
     * 水印文件对象
     */
    private File markFile;
    /**
     * 水印位置
     */
    private String locate;


    /**
     * 需替换参数
     */
    private Map<String, String> data;


    @SneakyThrows
    public File getMarkFile() {
        if (markFile == null){
            markFile = getMarkPng();
        }
        return markFile;
    }

    public void setMarkFile(File markFile) {
        this.markFile = markFile;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public int getPngWidth() {
        return pngWidth;
    }

    public void setPngWidth(int pngWidth) {
        this.pngWidth = pngWidth;
    }

    public int getPngHeight() {
        return pngHeight;
    }

    public void setPngHeight(int pngHeight) {
        this.pngHeight = pngHeight;
    }

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }

    public String getLocate() {
        return locate;
    }

    public void setLocate(String locate) {
        this.locate = locate;
    }

    public static FirstPageMark get(Map<String, String> mapMark) {
        FirstPageMark firstPageMark = new FirstPageMark();

        firstPageMark.setBase64(MapUtil.getStr(mapMark, "base64", null));
        firstPageMark.setTemplate(
                System.getProperty("user.dir") +
                        "/watermark/" +
                        MapUtil.getStr(mapMark, "template", "")
        );
        firstPageMark.setPngWidth(MapUtil.getInt(mapMark, "pngWidth", 0));
        firstPageMark.setPngHeight(MapUtil.getInt(mapMark, "pngHeight", 0));
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
     * 根据传入的“归档章对象”中的信息，生成水印PNG文件
     *
     * @return 水印图片
     * @throws IOException err
     */
    public File getMarkPng() throws IOException {
        // 生成归档章png图片
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
                this.getPngWidth(), this.getPngHeight(), this.getData());
    }


    /**
     * PDF文件中加入首页水印
     *
     * @param pdExtGfxState
     * @param contentStream
     * @param pdDocument    PDF文档对象
     * @param page          PDF页面对象
     * @param firstPageMark 归档章水印对象
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
        // 获取归档章png图片
        File filePng = firstPageMark.getMarkFile();

        if (filePng != null && filePng.exists()) {
            // 获取PDF首页的尺寸大小
            float floatPageWidth = page.getMediaBox().getWidth();
            float floatPageHeight = page.getMediaBox().getHeight();
            // 获取水印图片高度、宽度
            int intPngWidth = firstPageMark.getPngWidth();
            int intPngHeight = firstPageMark.getPngHeight();

            float floatIconLocateX = (floatPageWidth - intPngWidth / 5) / 2;
            float floatIconLocateY = floatPageHeight - intPngHeight / 5 - 10;

            switch (firstPageMark.getLocate().toUpperCase()){
                case "TR":
                    floatIconLocateX = floatPageWidth - intPngWidth / 5 - 10;
                    break;
                case "TL":
                    floatIconLocateX = 10;
                    break;
                case "CR":
                    floatIconLocateX = floatPageWidth - intPngWidth / 5 - 10;
                    floatIconLocateY = (floatPageHeight - intPngHeight / 5) / 2;
                    break;
                case "C":
                    floatIconLocateX = (floatPageWidth - intPngWidth / 5) / 2;
                    floatIconLocateY = (floatPageHeight - intPngHeight / 5) / 2;
                    break;
                case "CL":
                    floatIconLocateX = 10;
                    floatIconLocateY = (floatPageHeight - intPngHeight / 5) / 2;
                    break;
                case "BR":
                    floatIconLocateX = floatPageWidth - intPngWidth / 5 - 10;
                    floatIconLocateY = 10;
                    break;
                case "BM":
                    floatIconLocateX = (floatPageWidth - intPngWidth / 5) / 2;
                    floatIconLocateY = 10;
                    break;
                case "BL":
                    floatIconLocateX = 10;
                    floatIconLocateY = 10;
            }

            PngMark pngMark = new PngMark();
            pngMark.setWaterMarkFile(filePng.getAbsolutePath());
            pngMark.setLocateX(floatIconLocateX);
            pngMark.setLocateY(floatIconLocateY);
            pngMark.setImageWidth(intPngWidth / 5);
            pngMark.setImageHeight(intPngHeight / 5);

            pngMark.mark4Pdf(pdExtGfxState,
                    contentStream,
                    pdDocument,
                    page,
                    pngMark,
                    modifyX,
                    true,
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
        // 生成归档章png图片
        File filePng = firstPageMark.getMarkFile();

        if (filePng != null && filePng.exists()) {
            // 获取OFD页面大小
            double dblPageWidth = pageSize.getWidth();
            double dblPageHeight = pageSize.getHeight();
            // 获取水印图片高度、宽度
            int intPngWidth = firstPageMark.getPngWidth();
            int intPngHeight = firstPageMark.getPngHeight();

            float floatIconLocateX = (float) (dblPageWidth - intPngWidth / 12) / 2;
            float floatIconLocateY = (float) (dblPageHeight - intPngHeight / 12 - 5);

            switch (firstPageMark.getLocate().toUpperCase()){
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
     * @param firstPageMark 归档章对象
     */
    public void mark4Jpg(String strInputJpg, String strOutputJpg,
                         FirstPageMark firstPageMark) throws Exception {
        File fileSourceImg = new File(strInputJpg);
        BufferedImage buffSourceImg = ImageIO.read(new FileInputStream(fileSourceImg));
        BufferedImage buffImg = new BufferedImage(buffSourceImg.getWidth(), buffSourceImg.getHeight(), BufferedImage.TYPE_INT_RGB);
        // 获取图片的大小
        int intImageWidth = buffImg.getWidth();

        // 获取水印图片高度、宽度
        int intPngWidth = firstPageMark.getPngWidth();
        int intPngHeight = firstPageMark.getPngHeight();

        // 计算水印图片在右上角的坐标
        int intIconLocateX = (intImageWidth - intPngWidth) / 2;
        int intIconLocateY = 100;

        File filePng = firstPageMark.getMarkFile();

        if (filePng.exists()) {
            WaterMarkUtil.markImageByImage(filePng.getAbsolutePath(),
                    strInputJpg, strOutputJpg,
                    0,
                    intIconLocateX, intIconLocateY,
                    intPngWidth, intPngHeight);
            FileUtil.del(filePng);
        }

    }

}
