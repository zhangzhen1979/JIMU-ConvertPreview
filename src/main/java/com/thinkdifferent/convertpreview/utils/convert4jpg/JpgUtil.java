package com.thinkdifferent.convertpreview.utils.convert4jpg;

import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import com.thinkdifferent.convertpreview.entity.Thumbnail;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * 图片转换工具。
 * 各种格式转换为Jpg文件。
 * Jpg文件的处理（缩略图等）
 */
@Order
@Component
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
            throws IOException {
        Thumbnails.of(strInputFilePath).
                //scalingMode(ScalingMode.BICUBIC).
                        scale(dblScale). // 图片缩放80%, 不能和size()一起使用
                outputQuality(dblQuality). // 图片质量压缩80%
                toFile(strOutputFilePath);
        return new File(strOutputFilePath);
    }

    public File getThumbnail(ConvertEntity convertEntity, List<String> listJpg)
            throws IOException {
        File fileOut = null;
        Thumbnail thumbnail = convertEntity.getThumbnail();
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
            } catch (Exception e) {
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
                    convertEntity.getWriteBack().getOutputPath() + convertEntity.getOutPutFileName() + ".jpg",
                    thumbnail.getWidth(),
                    thumbnail.getHeight()
            );

        } else if (thumbnail.getScale() >= 0d) {
            // 如果输入了比例，则按比例生成
            fileOut = thumbnail(
                    listJpg.get(0),
                    convertEntity.getWriteBack().getOutputPath() + convertEntity.getOutPutFileName() + ".jpg",
                    thumbnail.getScale(),
                    thumbnail.getQuality()
            );

        }

        return fileOut;
    }

}
