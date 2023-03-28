package com.thinkdifferent.convertpreview.utils.convert4jpg;

import cn.hutool.core.img.Img;
import cn.hutool.core.img.ImgUtil;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.twelvemonkeys.io.FileSeekableStream;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Log4j2
public class ImgUtil2Jpg extends ConvertJpg {

    @Override
    public List<String> convert(String strInputFile, String strOutputFile) {

        List<String> listImageFiles = new ArrayList<>();
        String strFilePrefix = strOutputFile.substring(strOutputFile.lastIndexOf("/") + 1, strOutputFile.lastIndexOf("."));

        try {

            File fileImg = new File(strInputFile);
            @Cleanup ImageInputStream isb = ImageIO.createImageInputStream(fileImg);
            Iterator<ImageReader> iterator = ImageIO.getImageReaders(isb);
            if (iterator == null || !iterator.hasNext()) {
                throw new IOException("Image file format not supported by ImageIO: ");
            }

            ImageReader reader = iterator.next();
            reader.setInput(isb);
            int intPicCount = reader.getNumImages(true);
            log.info("该文件共有【" + intPicCount + "】页");

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

            String strJpg;
            if (intPicCount == 1) {
                strJpg = strJpgPath + "/" + strFilePrefix + ".jpg";
                tif2Jpg(strInputFile, strJpg);

                listImageFiles.add(strJpg);
            } else {
                // 循环，处理多页tif文件，转换为jpg
                for (int i = 0; i < intPicCount; i++) {
                    strJpg = strJpgPath + "/" + i + ".jpg";
                    File fileJpg = new File(strJpg);
                    BufferedImage buffTiff = reader.read(i);

                    @Cleanup ImageOutputStream imageOutputStream = new FileImageOutputStream(fileJpg);
                    BufferedImage bufferedImage = ImgUtil.toBufferedImage(buffTiff, buffTiff.getType());
                    ImageWriter writer = ImgUtil.getWriter(bufferedImage, "jpg");
                    ImgUtil.write(bufferedImage, writer, imageOutputStream, -1F);

                    log.info("每页分别保存至： " + fileJpg.getCanonicalPath());

                    listImageFiles.add(strJpg);
                }
            }

        } catch (Exception e) {
            log.error("图片转换为jpg异常：", e);
        }

        return listImageFiles;
    }

    /**
     * 单页tif文件转换处理
     *
     * @param imgPath 输入文件路径和文件名
     * @param jpgPath 转换后jpg文件路径和文件名
     * @return 转换后的jpg文件File对象
     */
    private File tif2Jpg(String imgPath, String jpgPath) {
        File fileImg = new File(imgPath);
        File fileJpg = new File(jpgPath);

        try {
            BufferedImage buffTiff = ImageIO.read(fileImg);

            if(buffTiff.getColorModel().getPixelSize() > 24){
                // 色深32位的tif图，直接用hutool的Img对象转
                @Cleanup ImageOutputStream out = new FileImageOutputStream(fileJpg);
                Img.from(buffTiff).write(out);
            }else{
                // 色深24位图转的方式
                @Cleanup FileSeekableStream fileSeekStream = new FileSeekableStream(fileImg);
                String strFileExt = imgPath.substring(imgPath.lastIndexOf(".") + 1).toUpperCase();
                ImageDecoder dec = ImageCodec.createImageDecoder(getPicType(strFileExt), fileSeekStream, null);
                RenderedImage renderedImage = dec.decodeAsRenderedImage(0);

                ParameterBlock pb = new ParameterBlock();
                pb.addSource(renderedImage);
                pb.add(fileJpg.toString());
                pb.add("JPEG");
                pb.add(null);
                RenderedOp renderedOp = JAI.create("filestore", pb);
                renderedOp.dispose();
            }

            return fileJpg;
        } catch (Exception e) {
            log.error(e);
        }

        return null;
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

    /**
     * 是否匹配
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        if (StringUtils.equalsAnyIgnoreCase(input,
                "tif", "tiff",
                "gif", "jpg", "jpeg")) {
            return true;
        } else {
            return false;
        }
    }


}
