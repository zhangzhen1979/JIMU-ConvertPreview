package com.thinkdifferent.convertpreview.utils.imgUtil;

import com.alibaba.simpleimage.ImageWrapper;
import com.alibaba.simpleimage.util.ImageReadHelper;
import com.thinkdifferent.convertpreview.entity.ConvertDocEntity;
import com.thinkdifferent.convertpreview.entity.Thumbnail;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 图片转换工具。
 * 各种格式转换为Jpg文件。
 * Jpg文件的处理（缩略图等）
 */
@Log4j2
public class JpgUtil {

    /*************************** 图片处理相关方法 **************************/

    /**
     * 使用给定的图片生成指定大小的图片（原格式）
     *
     * @param strInputFilePath  输入文件的绝对路径和文件名
     * @param strOutputFilePath 输出文件的绝对路径和文件名
     * @param intWidth          输出文件的宽度
     * @param intHeight         输出文件的高度
     */
    public File fixedSizeImage(String strInputFilePath, String strOutputFilePath,
                               int intWidth, int intHeight)
            throws IOException {
        Thumbnails.of(strInputFilePath).
                size(intWidth, intHeight).
                toFile(strOutputFilePath);
        return new File(strOutputFilePath);
    }

    /**
     * 按比例缩放图片
     *
     * @param strInputFilePath  输入文件的绝对路径和文件名
     * @param strOutputFilePath 输出文件的绝对路径和文件名
     * @param dblScale          输出文件的缩放百分比。1为100%,0.8为80%，以此类推。
     * @param dblQuality        输出文件的压缩比（质量）。1为100%,0.8为80%，以此类推。
     */
    public File thumbnail(String strInputFilePath, String strOutputFilePath,
                          double dblScale, double dblQuality)
            throws Exception {

        @Cleanup
        InputStream is = new FileInputStream(new File(strInputFilePath));
        ImageWrapper imageWrapper = ImageReadHelper.read(is);
        Thumbnails.Builder<? extends BufferedImage> builder =
                Thumbnails.of(imageWrapper.getAsBufferedImage()).
                        scale(dblScale).
                        outputQuality(dblQuality).// 图片质量压缩80%
                        imageType(BufferedImage.TYPE_INT_RGB);
        BufferedImage bufferedImage = builder.asBufferedImage();

        File fileOut = new File(strOutputFilePath);
        ImageIO.write(bufferedImage,"JPEG", fileOut);

        return fileOut;
    }

    public File getThumbnail(ConvertDocEntity convertDocEntity, List<String> listJpg)
            throws Exception {
        File fileOut = null;
        Thumbnail thumbnail = convertDocEntity.getThumbnail();
        if (thumbnail.getWidth() > 0 || thumbnail.getHeight() > 0) {
            // 如果输入了边长，则按边长生成
            int intImageWidth = 0;
            int intImageHeight = 0;
            try (FileInputStream fis = new FileInputStream(new File(listJpg.get(0)))) {
                BufferedImage buffSourceImg = ImageIO.read(fis);
                BufferedImage buffImg = new BufferedImage(buffSourceImg.getWidth(), buffSourceImg.getHeight(), BufferedImage.TYPE_INT_RGB);
                // 获取图片的大小
                intImageWidth = buffImg.getWidth();
                intImageHeight = buffImg.getHeight();
            } catch (Exception | Error e) {
                e.printStackTrace();
            }

            if (thumbnail.getWidth() > 0 && thumbnail.getHeight() == 0) {
                // 如果只输入了宽，则按比例计算高
                thumbnail.setHeight(thumbnail.getWidth() * intImageHeight / intImageWidth);
            } else if (thumbnail.getWidth() == 0 && thumbnail.getHeight() > 0) {
                // 如果只输入了高，则按比例计算宽
                thumbnail.setWidth(thumbnail.getHeight() * intImageWidth / intImageHeight);
            }

            fileOut = fixedSizeImage(
                    listJpg.get(0),
                    convertDocEntity.getWriteBack().getOutputPath() + convertDocEntity.getOutPutFileName() + ".jpg",
                    thumbnail.getWidth(),
                    thumbnail.getHeight()
            );

        } else if (thumbnail.getScale() >= 0d) {
            // 如果输入了比例，则按比例生成
            fileOut = thumbnail(
                    listJpg.get(0),
                    convertDocEntity.getWriteBack().getOutputPath() + convertDocEntity.getOutPutFileName() + ".jpg",
                    thumbnail.getScale(),
                    thumbnail.getQuality()
            );

        }

        return fileOut;
    }


    /**
     * 将图片缓存保存为文件
     *
     * @param buffImage    输入的图片对象
     * @param strOutputJpg 输出的JPG文件路径和文件名
     * @return 转换成功的JPG文件File对象
     */
    public static File bufferedImage2Jpg(BufferedImage buffImage, String strOutputJpg) throws IOException {
        //文件储存对象
        strOutputJpg = SystemUtil.beautifulFilePath(strOutputJpg);
        File filePath = new File(strOutputJpg.substring(0, strOutputJpg.lastIndexOf("/")));
        if (!filePath.exists()) {
            filePath.mkdirs();
        }

        File fileJpg = new File(strOutputJpg);

        ImageOutputStream out = new FileImageOutputStream(fileJpg);
        ImageIO.write(buffImage, "jpg", fileJpg);
//        ImgUtil.convertPdf(buffImage, "jpg", out, true);
        out.close();
        if (buffImage != null) {
            buffImage.getGraphics().dispose();
        }

        return fileJpg;
    }

}
