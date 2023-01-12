package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import com.sun.media.jai.codec.*;
import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import com.thinkdifferent.convertpreview.entity.Thumbnail;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 图片转换工具。
 * 各种格式转换为Jpg文件。
 * Jpg文件的处理（缩略图等）
 */
@Order
@Component
@Log4j2
public class ConvertJpgUtil {

    /**
     * 图片 转  JPG。
     * 支持输入格式如下：BMP、GIF、FlashPix、JPEG、PNG、PMN、TIFF、WBMP
     *
     * @param strInputFile  输入文件的路径和文件名
     * @param strOutputFile 输出文件的路径和文件名
     * @return
     */
    public List<String> convertPic2Jpg(String strInputFile, String strOutputFile)
            throws IOException {
        List<String> listImageFiles = new ArrayList<>();
        Assert.isTrue(StringUtils.isNotBlank(strInputFile), "文件名为空");
        File inputFile = new File(strInputFile);
        Assert.isTrue(inputFile.exists(), "找不到文件【" + strInputFile + "】");
        strInputFile = SystemUtil.beautifulFilePath(strInputFile);
        strOutputFile = SystemUtil.beautifulFilePath(strOutputFile);

        try (FileInputStream fis = new FileInputStream(inputFile)){
            // create file
            FileUtil.touch(strOutputFile);
            BufferedImage image = ImageIO.read(fis);
            int width = image.getWidth();
            int height = image.getHeight();

            Thumbnails.of(inputFile)
                    .size(width, height)
                    .outputFormat("jpg")
                    .toFile(strOutputFile);
            image = null;
            // inputFile.delete();
            return Collections.singletonList(strOutputFile);
        } catch (Exception e) {
            log.error("转换单页jpg出现问题，使用旧方法", e);
        }

        // 老办法，解决Thumbnails组件部分格式不兼容的问题。
        String strFilePrefix = strOutputFile.substring(strOutputFile.lastIndexOf("/") + 1, strOutputFile.lastIndexOf("."));
        String strFileExt = strInputFile.substring(strInputFile.lastIndexOf(".") + 1).toUpperCase();

        @Cleanup FileSeekableStream fileSeekStream = new FileSeekableStream(strInputFile);

        ImageDecoder imageDecoder = ImageCodec.createImageDecoder(getPicType(strFileExt), fileSeekStream, null);
        int intPicCount = imageDecoder.getNumPages();
        log.info("该" + strFileExt + "文件共有【" + intPicCount + "】页");

        String strJpgPath = "";
        if (intPicCount == 1) {
            // 如果是单页tif文件，则转换的目标文件夹就在指定的位置
            strJpgPath = strOutputFile.substring(0, strOutputFile.lastIndexOf("/"));
        } else {
            // 如果是多页tif文件，则在目标文件夹下，按照文件名再创建子目录，将转换后的文件放入此新建的子目录中
            strJpgPath = strOutputFile.substring(0, strOutputFile.lastIndexOf("."));
        }

        // 处理目标文件夹，如果不存在则自动创建
        File fileJpgPath = new File(strJpgPath);
        if (!fileJpgPath.exists()) {
            fileJpgPath.mkdirs();
        }

        PlanarImage in = JAI.create("stream", fileSeekStream);
        // OutputStream os = null;
        JPEGEncodeParam param = new JPEGEncodeParam();

        // 循环，处理每页tif文件，转换为jpg
        for (int i = 0; i < intPicCount; i++) {
            String strJpg;
            if (intPicCount == 1) {
                strJpg = strJpgPath + "/" + strFilePrefix + ".jpg";
            } else {
                strJpg = strJpgPath + "/" + i + ".jpg";
            }

            File fileJpg = new File(strJpg);
            @Cleanup OutputStream os = new FileOutputStream(strJpg);
            ImageEncoder enc = ImageCodec.createImageEncoder("JPEG", os, param);
            enc.encode(in);

            log.info("每页分别保存至： " + fileJpg.getCanonicalPath());

            listImageFiles.add(strJpg);
        }

        return listImageFiles;
    }

    private String getPicType(String strExt) {
        switch (strExt.toUpperCase()) {
            case "JPG":
                return "JPEG";
            case "TIF":
                return "TIFF";
            default:
                return strExt.toUpperCase();
        }
    }


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
