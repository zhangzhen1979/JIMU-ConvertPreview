package com.thinkdifferent.convertpreview.utils.convert4jpg;

import cn.hutool.core.io.FileUtil;
import com.sun.media.jai.codec.*;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
public class Img2Jpg extends ConvertJpg {

    @Override
    public List<String> convert(String strInputFile, String strOutputFile)
            throws IOException{
        List<String> listImageFiles = new ArrayList<>();
        File inputFile = new File(strInputFile);

        String strFileExt = strInputFile.substring(strInputFile.lastIndexOf(".") + 1).toUpperCase();

        try (FileInputStream fis = new FileInputStream(inputFile)){
            // create file
            FileUtil.touch(strOutputFile);
            BufferedImage image = ImageIO.read(fis);
            if(image != null){
                int width = image.getWidth();
                int height = image.getHeight();

                Thumbnails.of(inputFile)
                        .size(width, height)
                        .outputFormat("jpg")
                        .toFile(strOutputFile);

                image = null;
            }

            return Collections.singletonList(strOutputFile);
        } catch (Exception e) {
            log.error("图片转换为jpg出现问题，使用旧方法", e);
        }

        // 老办法，解决Thumbnails组件部分格式不兼容的问题。
        String strFilePrefix = strOutputFile.substring(strOutputFile.lastIndexOf("/") + 1, strOutputFile.lastIndexOf("."));

        @Cleanup FileSeekableStream fileSeekStream = new FileSeekableStream(strInputFile);

        try{
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
        }catch (Exception e){
            log.error("图片转换为jpg异常：", e);
        }

        return listImageFiles;
    }


    /**
     * 根据图片文件扩展名，获取图片类型（用于转换格式工具，作为原始格式参数）
     * @param strExt  图片文件扩展名
     * @return        文件类型（大写）
     */
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


    /**
     * 是否匹配
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        // 获取配置文件中设置的，本服务支持的图片文件扩展名
        String strPicType = ConvertConfig.picType;
        log.debug("picType:{}", strPicType);
        // 图片文件类型转换为数组
        String[] strsPicType = strPicType.split(",");

        if (StringUtils.equalsAnyIgnoreCase(input, strsPicType)){
            return true;
        }else{
            return false;
        }
    }


}
