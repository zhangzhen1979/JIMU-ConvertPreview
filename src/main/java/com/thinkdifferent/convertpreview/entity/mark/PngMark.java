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
    private static float scale = 0.5f;

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
     * PDF页面中添加图片水印
     * @param pdExtGfxState
     * @param contentStream
     * @param pdDocument        PDF文档对象
     * @param page              PDF页面对象
     * @param pngMark           图片水印对象
     * @param modifyX
     * @param alpha             透明度
     * @throws IOException
     */
    public void mark4Pdf(PDExtendedGraphicsState pdExtGfxState,
                         PDPageContentStream contentStream,
                         PDDocument pdDocument,
                         PDPage page,
                         PngMark pngMark,
                         float modifyX,
                         float alpha) throws IOException {
        pdExtGfxState.setNonStrokingAlphaConstant(alpha);
        pdExtGfxState.setAlphaSourceFlag(true);
        contentStream.setGraphicsStateParameters(pdExtGfxState);

        double dblIconLocateX;
        double dblIconLocateY;
        double doublePngWidthPt;
        double doublePngHeightPt;

        // 获取图片宽度pt（磅）
        int intPngWidthPx = pngMark.getImageWidth();
        if (intPngWidthPx == 0) {
            intPngWidthPx = 50;
        }
        doublePngWidthPt = intPngWidthPx * 72 / dpi;

        // 获取图片高度pt（磅）
        int intPngHeightPx = pngMark.getImageHeight();
        if (intPngHeightPx == 0) {
            intPngHeightPx = 50;
        }
        doublePngHeightPt = intPngHeightPx * 72 / dpi;

        // 计算图片X轴位置pt（磅）
        float floatIconLocateX = pngMark.getLocateX();
        if (floatIconLocateX == 0f) {
            floatIconLocateX = 40f;
        }
        dblIconLocateX = floatIconLocateX * 2.66;

        // 计算图片Y轴位置pt（磅）
        float floatIconLocateY = pngMark.getLocateY();
        if (floatIconLocateY == 0) {
            floatIconLocateY = 10;
        }
        dblIconLocateY = floatIconLocateY * 2.66;

        PDImageXObject pdImage = PDImageXObject.createFromFile(pngMark.getWaterMarkFile(), pdDocument);
        int rotation = page.getRotation();
        if (rotation > 0) {
            float floatLocalX = page.getMediaBox().getHeight() - (float)dblIconLocateY;
            float floatLocalY = page.getMediaBox().getWidth() - (float)dblIconLocateX - 60;
            // 通过Matrix.getRotateInstance(angle, x, y)进行转换会围绕坐标系的原点(0，0)旋转(通常是页面的左下角)，然后平移(x，y)。
            contentStream.transform(
                    Matrix.getRotateInstance(Math.toRadians(rotation),
                            (float)dblIconLocateY,
                            (float)dblIconLocateX
                    ));
            // tx、ty为图片绘制时的左上角坐标点。（需要累加前面的偏移量）
            contentStream.drawImage(pdImage,
                    floatLocalX - modifyX,
                    floatLocalY,
                    (float)doublePngWidthPt, (float)doublePngHeightPt
            );
        } else {
            contentStream.transform(
                    Matrix.getRotateInstance(Math.toRadians(0),
                            (float)dblIconLocateX,
                            (float)dblIconLocateY)
            );
            // tx、ty为图片绘制时的左上角坐标点。（需要累加前面的偏移量）
            contentStream.drawImage(pdImage,
                    (float)dblIconLocateX - modifyX,
                    (float)dblIconLocateY,
                    (float)doublePngWidthPt, (float)doublePngHeightPt
            );
        }

        // 反向旋转
        contentStream.transform(
                Matrix.getRotateInstance(Math.toRadians(0),
                        -1 * (float)dblIconLocateX,
                        -1 * (float)dblIconLocateY
                )
        );

        contentStream.restoreGraphicsState();

    }


    /**
     * 为OFD添加图片水印
     * @param ofdDoc        OFD页面对象
     * @param pageSize      页面尺寸对象
     * @param pngMark       图片水印对象
     * @param intPageNum    当前处理的页码
     * @param alpha         透明度
     * @throws IOException
     */
    public void mark4Ofd(OFDDoc ofdDoc,
                         ST_Box pageSize,
                         PngMark pngMark,
                         int intPageNum,
                         float alpha) throws IOException {
        Path pathIcon = Paths.get(pngMark.getWaterMarkFile());

        double dblPageWidth = pageSize.getWidth();
        double dblPageHeight = pageSize.getHeight();

        // 输入参数为mm
        // 声明每页上需要绘制的水印，以及水印位置
        Annotation annotation = new Annotation(
                new ST_Box(0d, 0d,
                        dblPageWidth, dblPageHeight),
                AnnotType.Watermark, ctx -> {
            ctx.setGlobalAlpha((double)alpha);
            ctx.save();
            //ctx.rotate(-1 * pngMark.getDegree());
            ctx.drawImage(pathIcon,
                    pngMark.getLocateX(), (dblPageHeight - pngMark.getLocateY() - pngMark.getImageHeight()),
                    pngMark.getImageWidth(), pngMark.getImageHeight());
            ctx.restore();
        });

        // 绘制水印
        ofdDoc.addAnnotation(intPageNum, annotation);

    }


    /**
     * 给JPG图片文件添加png图片水印
     * @param strInputJpg   输入的JPG文件路径和文件名
     * @param strOutPutJpg   输入的JPG文件路径和文件名
     * @param pngMark  图片水印对象
     */
    public void mark4Jpg(String strInputJpg, String strOutPutJpg, PngMark pngMark) throws Exception {
        JpgWaterMarkUtil.markImageByImage(pngMark.waterMarkFile,
                strInputJpg, strOutPutJpg,
                0,
                (int)pngMark.getLocateX(), (int)pngMark.getLocateY(),
                pngMark.imageWidth, pngMark.getImageHeight());
    }

}
