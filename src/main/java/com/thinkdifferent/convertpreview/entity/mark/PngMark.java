package com.thinkdifferent.convertpreview.entity.mark;

import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.utils.watermark.JpgWaterMarkUtil;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.edit.Annotation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 图片水印（PNG）
 */
@Log4j2
@Data
public class PngMark {
    /**
     * 水印图片路径, “watermark”文件夹中已经存放的水印文件路径
     */
    private String waterMarkFile;
    /**
     * 水印的位置，横轴 默认值为宽度的1/6位置（左下角为原点）
     */
    private float locateX;
    /**
     * 水印的位置，纵轴 默认值为高度的1/6位置（左下角为原点）
     */
    private float locateY;
    /**
     * 水印图片的宽度 默认值为150
     */
    private Integer imageWidth;
    /**
     * 水印图片的高度 默认值为150
     */
    private Integer imageHeight;

    private static int dpi = 400;

    /**
     * 根据传入的参数，转换为图片水印的对象
     *
     * @param mapMark 传入参数中“水印”部分的内容
     * @return
     */
    public PngMark get(Map<String, String> mapMark) {
        PngMark pngMark = new PngMark();
        // “watermark”文件夹中已经存放的水印文件
        pngMark.setWaterMarkFile(System.getProperty("user.dir") + "/watermark/" +
                MapUtil.getStr(mapMark, "waterMarkFile"));
        // 水印在文档中横轴|纵轴的位置
        pngMark.setLocateX(MapUtil.getInt(mapMark, "LocateX"));
        pngMark.setLocateY(MapUtil.getInt(mapMark, "LocateY"));
        // 水印图片大小
        pngMark.setImageWidth(MapUtil.getInt(mapMark, "imageWidth"));
        pngMark.setImageHeight(MapUtil.getInt(mapMark, "imageHeight"));

        return pngMark;

    }

    /**
     * 根据传入的参数，计算图片水印在PDF中的位置坐标
     *
     * @param strLocate          位置
     * @param floatProportion    放大倍数
     * @param doublePageHeightPt 页面高度Pt
     * @param dblPngHeightPt     水印图片高度Pt
     * @param doublePageWidthPt  页面宽度Pt
     * @param dblPngWidthPt      水印图片宽度Pt
     * @param rotation           旋转角度
     * @return 计算后的图片坐标值PngMarkLocal对象
     */
    public PngMarkLocal getPngLocateInPdf(String strLocate, float floatProportion,
                                          double doublePageHeightPt, double dblPngHeightPt,
                                          double doublePageWidthPt, double dblPngWidthPt,
                                          int rotation) {
        PngMarkLocal pngMarkLocal = new PngMarkLocal();

        // PDF原点为：左下角；页面尺寸：Pt（磅）
        float floatIconLocateX = 0f;
        float floatIconLocateY = 0f;

        if(rotation == 90 || rotation == 270){
            switch (strLocate) {
                case "TL":
                    strLocate = "TR";
                    break;
                case "TR":
                    strLocate = "TL";
                    break;
                case "CL":
                    strLocate = "CR";
                    break;
                case "CR":
                    strLocate = "CL";
                    break;
                case "BL":
                    strLocate = "BR";
                    break;
                case "BR":
                    strLocate = "BL";
            }

        }


        switch (strLocate) {
            case "TL":
                floatIconLocateX = 10 * floatProportion;
                floatIconLocateY = (float) ((doublePageHeightPt - dblPngHeightPt) - 10 * floatProportion);
                break;
            case "TM":
                floatIconLocateX = (float) ((doublePageWidthPt - dblPngWidthPt) * 0.5);
                floatIconLocateY = (float) ((doublePageHeightPt - dblPngHeightPt) - 10 * floatProportion);
                break;
            case "TR":
                floatIconLocateX = (float) ((doublePageWidthPt - dblPngWidthPt) - 10 * floatProportion);
                floatIconLocateY = (float) ((doublePageHeightPt - dblPngHeightPt) - 10 * floatProportion);
                break;
            case "CL":
                floatIconLocateX = 10 * floatProportion;
                floatIconLocateY = (float) ((doublePageHeightPt - dblPngHeightPt) * 0.5);
                break;
            case "C":
                floatIconLocateX = (float) ((doublePageWidthPt - dblPngWidthPt) * 0.5);
                floatIconLocateY = (float) ((doublePageHeightPt - dblPngHeightPt) * 0.5);
                break;
            case "CR":
                floatIconLocateX = (float) ((doublePageWidthPt - dblPngWidthPt) - 10 * floatProportion);
                floatIconLocateY = (float) ((doublePageHeightPt - dblPngHeightPt) * 0.5);
                break;
            case "BL":
                floatIconLocateX = 10 * floatProportion;
                floatIconLocateY = 10 * floatProportion;
                break;
            case "BM":
                floatIconLocateX = (float) ((doublePageWidthPt - dblPngWidthPt) * 0.5);
                floatIconLocateY = 10 * floatProportion;
                break;
            case "BR":
                floatIconLocateX = (float) ((doublePageWidthPt - dblPngWidthPt) - 10 * floatProportion);
                floatIconLocateY = 10 * floatProportion;
        }

        pngMarkLocal.setLocateX(floatIconLocateX);
        pngMarkLocal.setLocateY(floatIconLocateY);

        return pngMarkLocal;
    }


    /**
     * PDF页面中添加图片水印
     *
     * @param pdExtGfxState
     * @param contentStream
     * @param pdDocument    PDF文档对象
     * @param page          PDF页面对象
     * @param pngMark       图片水印对象
     * @param strLocal      图片水印位置
     * @param alpha         透明度
     * @throws IOException
     */
    public void mark4Pdf(PDExtendedGraphicsState pdExtGfxState,
                         PDPageContentStream contentStream,
                         PDDocument pdDocument,
                         PDPage page,
                         PngMark pngMark,
                         String strLocal,
                         float alpha) throws IOException {
        if (pngMark != null) {
            pdExtGfxState.setNonStrokingAlphaConstant(alpha);
            pdExtGfxState.setAlphaSourceFlag(true);
            contentStream.setGraphicsStateParameters(pdExtGfxState);

            // 获取水印图片高度、宽度（px）
            double doublePngWidth = pngMark.getImageWidth();
            double doublePngHeight = pngMark.getImageHeight();
            // 获取图片宽度、高度pt（磅）
            double doublePngWidthPt = doublePngWidth * 72 / dpi;
            double doublePngHeightPt = doublePngHeight * 72 / dpi;

            // 页面高度 A4:841.92*595.32（磅）
            float floatPageHeightPt = page.getMediaBox().getHeight();
            // 页面宽度 A4:841.92*595.32（磅）
            float floatPageWidthPt = page.getMediaBox().getWidth();
            float minPageSize = Math.min(floatPageWidthPt, floatPageHeightPt);
            // 如果纸张不是A4，则按照比例缩放图片大小
            float floatProportion = 1f;
            if(minPageSize<595 || minPageSize>596){
                floatProportion = (float)(minPageSize/595.32);

                doublePngWidthPt = floatProportion * doublePngWidthPt;
                doublePngHeightPt = floatProportion * doublePngHeightPt;
            }

            // 获取页面角度
            int rotation = page.getRotation();
            // 如果页面旋转了90度或270度，则交换：页面高度、页面宽度的值
            if (rotation ==90 || rotation == 270) {
                float floatTmp = floatPageHeightPt;
                floatPageHeightPt = floatPageWidthPt;
                floatPageWidthPt = floatTmp;
            }

            // 计算图片位置
            if (strLocal != null) {
                strLocal = strLocal.toUpperCase();

                PngMarkLocal pngMarkLocal = pngMark.getPngLocateInPdf(strLocal, floatProportion,
                        floatPageHeightPt, doublePngHeightPt,
                        floatPageWidthPt, doublePngWidthPt,
                        rotation);
                pngMark.setLocateX(pngMarkLocal.getLocateX());
                pngMark.setLocateY(pngMarkLocal.getLocateY());
            }else{
                doublePngWidthPt = 16 * doublePngWidthPt;
                doublePngHeightPt = 16 * doublePngHeightPt;
            }

            PDImageXObject pdImage = PDImageXObject.createFromFile(pngMark.getWaterMarkFile(), pdDocument);

            // tx、ty为图片绘制时的左上角坐标点。
            float tx = pngMark.getLocateX();
            float ty = pngMark.getLocateY();

            if (rotation ==90 || rotation == 270) {
                tx = pngMark.getLocateY();
                ty = (float)(pngMark.getLocateX() + doublePngWidthPt);
            }

            // 移动坐标原点，准备绘制图片。
            // 通过Matrix.getRotateInstance(angle, x, y)进行旋转会围绕坐标系的原点(0，0)旋转(通常是页面的左下角)，然后平移(x，y)。
            contentStream.transform(
                    Matrix.getRotateInstance(Math.toRadians(rotation),
                            tx,
                            ty
                    )
            );
            // 绘制图片
            contentStream.drawImage(pdImage,
                    0,
                    0,
                    (float) doublePngWidthPt, (float) doublePngHeightPt
            );

            // 反向旋转
            contentStream.transform(
                    Matrix.getRotateInstance(Math.toRadians(0),
                            -1 * pngMark.getLocateX(),
                            -1 * pngMark.getLocateY()
                    )
            );

            contentStream.restoreGraphicsState();
        }
    }

    /**
     * 根据传入的参数，计算图片水印在OFD中的位置坐标
     *
     * @param strLocate          位置
     * @param floatProportion    放大倍数
     * @param doublePageHeightMm 页面高度毫米mm
     * @param dblPngHeightMm     水印图片高度毫米mm
     * @param doublePageWidthMm  页面宽度毫米mm
     * @param dblPngWidthMm      水印图片宽度毫米mm
     * @return 计算后的图片坐标值PngMarkLocal对象
     */
    public PngMarkLocal getPngLocateInOfd(String strLocate, float floatProportion,
                                          double doublePageHeightMm, double dblPngHeightMm,
                                          double doublePageWidthMm, double dblPngWidthMm) {
        PngMarkLocal pngMarkLocal = new PngMarkLocal();
        // OFD原点为：左上角；页面尺寸：mm（毫米）
        float floatIconLocateX = 0f;
        float floatIconLocateY = 0f;

        switch (strLocate) {
            case "TL":
                floatIconLocateX = 5 * floatProportion;
                floatIconLocateY = 5 * floatProportion;
                break;
            case "TM":
                floatIconLocateX = (float) ((doublePageWidthMm - dblPngWidthMm) * 0.5);
                floatIconLocateY = 5 * floatProportion;
                break;
            case "TR":
                floatIconLocateX = (float) (doublePageWidthMm - dblPngWidthMm - 5 * floatProportion);
                floatIconLocateY = 5 * floatProportion;
                break;
            case "CL":
                floatIconLocateX = 5 * floatProportion;
                floatIconLocateY = (float) ((doublePageHeightMm - dblPngHeightMm) * 0.5);
                break;
            case "C":
                floatIconLocateX = (float) ((doublePageWidthMm - dblPngWidthMm) * 0.5);
                floatIconLocateY = (float) ((doublePageHeightMm - dblPngHeightMm) * 0.5);
                break;
            case "CR":
                floatIconLocateX = (float) (doublePageWidthMm - dblPngWidthMm - 5 * floatProportion);
                floatIconLocateY = (float) ((doublePageHeightMm - dblPngHeightMm) * 0.5);
                break;
            case "BL":
                floatIconLocateX = 5 * floatProportion;
                floatIconLocateY = (float) (doublePageHeightMm - dblPngHeightMm - 5 * floatProportion);
                break;
            case "BM":
                floatIconLocateX = (float) ((doublePageWidthMm - dblPngWidthMm) * 0.5);
                floatIconLocateY = (float) (doublePageHeightMm - dblPngHeightMm - 5 * floatProportion);
                break;
            case "BR":
                floatIconLocateX = (float) (doublePageWidthMm - dblPngWidthMm - 5 * floatProportion);
                floatIconLocateY = (float) (doublePageHeightMm - dblPngHeightMm - 5 * floatProportion);
        }

        pngMarkLocal.setLocateX(floatIconLocateX);
        pngMarkLocal.setLocateY(floatIconLocateY);

        return pngMarkLocal;
    }


    /**
     * 为OFD添加图片水印
     *
     * @param ofdDoc     OFD页面对象
     * @param pageSize   页面尺寸对象
     * @param pngMark    图片水印对象
     * @param strLocal   图片位置（非必填）
     * @param intPageNum 当前处理的页码
     * @param alpha      透明度
     * @throws IOException
     */
    public void mark4Ofd(OFDDoc ofdDoc,
                         ST_Box pageSize,
                         PngMark pngMark,
                         String strLocal,
                         int intPageNum,
                         float alpha) throws IOException {
        if (pngMark != null) {
            // 获取当前页纸张宽度。A4:297.015*210.019
            double doublePageWidthMm = pageSize.getWidth();
            // 获取当前页纸张高度。A4:297.015*210.019
            double doublePageHeightMm = pageSize.getHeight();
            // A4:210.019
            double minPageSize = Math.min(doublePageWidthMm, doublePageHeightMm);

            // 如果纸张不是A4，则按照比例缩放字体大小
            float floatProportion = 1f;
            if (minPageSize < 210 || minPageSize > 211) {
                floatProportion = (float) (minPageSize / 210);
            }

            // 获取水印图片高度、宽度
            double doublePngWidthMm = floatProportion * pngMark.getImageWidth().doubleValue();
            double doublePngHeightMm = floatProportion * pngMark.getImageHeight().doubleValue();

            if (strLocal != null) {
                strLocal = strLocal.toUpperCase();
                PngMarkLocal pngMarkLocal = pngMark.getPngLocateInOfd(strLocal, floatProportion,
                        doublePageHeightMm, doublePngHeightMm,
                        doublePageWidthMm, doublePngWidthMm);
                pngMark.setLocateX(pngMarkLocal.getLocateX());
                pngMark.setLocateY(pngMarkLocal.getLocateY());
            }else{
                pngMark.setLocateY((float)doublePageHeightMm - floatProportion * pngMark.getImageHeight() - pngMark.getLocateY());
            }

            pngMark.setImageWidth((int) doublePngWidthMm);
            pngMark.setImageHeight((int) doublePngHeightMm);

            Path pathIcon = Paths.get(pngMark.getWaterMarkFile());

            // 输入参数为mm
            // 声明每页上需要绘制的水印，以及水印位置
            double finalDoublePngWidth = doublePngWidthMm;
            double finalDoublePngHeight = doublePngHeightMm;
            Annotation annotation = new Annotation(
                    new ST_Box(0d, 0d,
                            doublePageWidthMm, doublePageHeightMm),
                    AnnotType.Watermark, ctx -> {
                ctx.setGlobalAlpha((double) alpha);
                ctx.save();
//                ctx.rotate(-1 * pngMark.getDegree());
                ctx.drawImage(pathIcon,
                        pngMark.getLocateX(),
                        pngMark.getLocateY(),
                        finalDoublePngWidth,
                        finalDoublePngHeight);
                ctx.restore();
            });

            // 绘制水印
            ofdDoc.addAnnotation(intPageNum, annotation);
        }
    }


    /**
     * 根据传入的参数，计算图片水印在JPG中的位置坐标
     *
     * @param strLocate          位置
     * @param doublePageHeightPx 页面高度像素px
     * @param dblPngHeightPx     水印图片高度像素px
     * @param doublePageWidthPx  页面宽度像素px
     * @param dblPngWidthPx      水印图片宽度像素px
     * @return 计算后的图片坐标值PngMarkLocal对象
     */
    public PngMarkLocal getPngLocateInJpg(String strLocate,
                                          double doublePageHeightPx, double dblPngHeightPx,
                                          double doublePageWidthPx, double dblPngWidthPx) {
        PngMarkLocal pngMarkLocal = new PngMarkLocal();
        // JPG（图片）原点为：左上角：页面尺寸：px（像素）

        float floatIconLocateX = 10f;
        float floatIconLocateY = 10f;

        switch (strLocate) {
            case "TL":
                floatIconLocateX = 10;
                floatIconLocateY = 10;
                break;
            case "TM":
                floatIconLocateX = (float) ((doublePageWidthPx - dblPngWidthPx) * 0.5);
                floatIconLocateY = 10;
                break;
            case "TR":
                floatIconLocateX = (float) (doublePageWidthPx - dblPngWidthPx - 20);
                floatIconLocateY = 10;
                break;
            case "CL":
                floatIconLocateX = 10;
                floatIconLocateY = (float) ((doublePageHeightPx - dblPngHeightPx) * 0.5);
                break;
            case "C":
                floatIconLocateX = (float) ((doublePageWidthPx - dblPngWidthPx) * 0.5);
                floatIconLocateY = (float) ((doublePageHeightPx - dblPngHeightPx) * 0.5);
                break;
            case "CR":
                floatIconLocateX = (float) (doublePageWidthPx - dblPngWidthPx - 10);
                floatIconLocateY = (float) ((doublePageHeightPx - dblPngHeightPx) * 0.5);
                break;
            case "BL":
                floatIconLocateX = 10;
                floatIconLocateY = (float) (doublePageHeightPx - dblPngHeightPx - 10);
                break;
            case "BM":
                floatIconLocateX = (float) ((doublePageWidthPx - dblPngWidthPx) * 0.5);
                floatIconLocateY = (float) (doublePageHeightPx - dblPngHeightPx - 10);
                break;
            case "BR":
                floatIconLocateX = (float) (doublePageWidthPx - dblPngWidthPx - 10);
                floatIconLocateY = (float) (doublePageHeightPx - dblPngHeightPx - 10);
        }

        pngMarkLocal.setLocateX(floatIconLocateX);
        pngMarkLocal.setLocateY(floatIconLocateY);

        return pngMarkLocal;
    }

    /**
     * 给JPG图片文件添加png图片水印
     *
     * @param strInputJpg  输入的JPG文件路径和文件名
     * @param strOutPutJpg 输入的JPG文件路径和文件名
     * @param pngMark      图片水印对象
     */
    public void mark4Jpg(String strInputJpg, String strOutPutJpg, PngMark pngMark) throws Exception {
        if (pngMark != null) {
            JpgWaterMarkUtil.markImageByImage(pngMark.waterMarkFile,
                    strInputJpg, strOutPutJpg,
                    0,
                    (int) pngMark.getLocateX(), (int) pngMark.getLocateY(),
                    pngMark.imageWidth, pngMark.getImageHeight());
        }
    }

}
