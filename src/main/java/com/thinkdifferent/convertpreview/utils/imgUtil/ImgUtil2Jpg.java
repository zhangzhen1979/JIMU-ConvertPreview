package com.thinkdifferent.convertpreview.utils.imgUtil;

import cn.hutool.core.img.Img;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
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
        String strFilePrefix = strOutputFile.substring(strOutputFile.lastIndexOf("/") + 1);

        BufferedImage buffTiff = null;
        BufferedImage bufferedImage = null;

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

            // 处理目标文件夹，如果不存在则自动创建
            File fileJpgPath = new File(strOutputFile);
            if (!fileJpgPath.exists()) {
                fileJpgPath.mkdirs();
            }

            String strJpg;
            if (intPicCount == 1) {
                strJpg = strOutputFile + "/" + strFilePrefix + ".jpg";
                tif2Jpg(strInputFile, strJpg);

                listImageFiles.add(strJpg);
            } else {
                // 循环，处理多页tif文件，转换为jpg
                for (int i = 0; i < intPicCount; i++) {
                    strJpg = strOutputFile + "/" + i + ".jpg";
                    File fileJpg = new File(strJpg);
                    buffTiff = reader.read(i);

                    bufferedImage = ImgUtil.toBufferedImage(buffTiff, buffTiff.getType());
                    ImageWriter writer = ImgUtil.getWriter(bufferedImage, "jpg");
                    @Cleanup ImageOutputStream imageOutputStream = new FileImageOutputStream(fileJpg);
                    ImgUtil.write(bufferedImage, writer, imageOutputStream, -1F);

                    log.info("每页分别保存至： " + fileJpg.getCanonicalPath());

                    listImageFiles.add(strJpg);
                }
            }

        } catch (Exception | Error e) {
            log.error("图片转换为jpg异常：", e);

        } finally {
            if (buffTiff != null) {
                buffTiff.getGraphics().dispose();
            }
            if (bufferedImage != null) {
                bufferedImage.getGraphics().dispose();
            }

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

        BufferedImage buffTiff = null;
        try {
            buffTiff = ImageIO.read(fileImg);

            if (buffTiff.getColorModel().getPixelSize() > 24) {
                // 色深32位的tif图，直接用hutool的Img对象转
                @Cleanup ImageOutputStream out = new FileImageOutputStream(fileJpg);
                Img.from(buffTiff).write(out);
            } else {
                // 色深24位图转的方式
                @Cleanup FileSeekableStream fileSeekStream = new FileSeekableStream(fileImg);
                String strImgExt = FileUtil.extName(imgPath).toUpperCase();

                String strPath = SystemUtil.beautifulFilePath(jpgPath);
                strPath = strPath.substring(0, strPath.lastIndexOf("/"));
                File filePath = new File(strPath);
                if(!filePath.exists() || !filePath.isDirectory()){
                    filePath.mkdirs();
                }

                ImageDecoder dec = ImageCodec.createImageDecoder(getPicType(strImgExt), fileSeekStream, null);
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
        } catch (Exception | Error e) {
            log.error(e);

        } finally {
            if (buffTiff != null) {
                buffTiff.getGraphics().dispose();
            }
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
        return StringUtils.equalsAnyIgnoreCase(input,
                "tif", "tiff", "jpg", "jpeg");
    }


}
