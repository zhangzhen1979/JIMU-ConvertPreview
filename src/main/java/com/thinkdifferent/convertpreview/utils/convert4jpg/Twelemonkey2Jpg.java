package com.thinkdifferent.convertpreview.utils.convert4jpg;

import com.thinkdifferent.convertpreview.config.ConvertConfig;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

@Log4j2
public class Twelemonkey2Jpg extends ConvertJpg {

    @Override
    public List<String> convert(String strInputFile, String strOutputFile) {

        File fileJpg = img2Jpg(strInputFile, strOutputFile);
        if (fileJpg != null && fileJpg.exists() && fileJpg.length() > 0) {
            return Collections.singletonList(strOutputFile);
        }

        return null;
    }


    private File img2Jpg(String imgPath, String jpgPath) {

        File inputFile = new File(imgPath);

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            BufferedImage image = ImageIO.read(fis);
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();

                Thumbnails.of(inputFile)
                        .size(width, height)
                        .outputFormat("jpg")
                        .toFile(jpgPath);

                return new File(jpgPath);
            }
        } catch (Exception e) {
            log.error("图片转换为jpg出现问题", e);
        }

        return null;

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
        // 图片文件类型转换为数组
        String[] strsPicType = strPicType.split(",");

        if (StringUtils.equalsAnyIgnoreCase(input, strsPicType)) {
            return true;
        } else {
            return false;
        }
    }

}
