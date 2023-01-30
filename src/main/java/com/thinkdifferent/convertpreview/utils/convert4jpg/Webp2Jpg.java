package com.thinkdifferent.convertpreview.utils.convert4jpg;

import com.luciad.imageio.webp.WebPReadParam;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Log4j2
public class Webp2Jpg extends ConvertJpg {

    @Override
    public List<String> convert(String strInputFile, String strOutputFile)
            throws IOException{

        File fileJpg = webp2Jpg(strInputFile, strOutputFile);
        if(fileJpg != null && fileJpg.exists() && fileJpg.length() > 0){
            return Collections.singletonList(strOutputFile);
        }

        return null;
    }


    private File webp2Jpg(String webpPath, String jpgPath) throws IOException {
        File fileWebp = new File(webpPath);
        File fileJpg = null;
        if(fileWebp != null && fileWebp.exists() && fileWebp.length() > 0){
            ImageReader reader = ImageIO.getImageReadersByMIMEType("image/webp").next();
            WebPReadParam readParam = new WebPReadParam();
            readParam.setBypassFiltering(true);

            @Cleanup FileImageInputStream is = new FileImageInputStream(fileWebp);
            reader.setInput(is);

            try {
                BufferedImage image = reader.read(0, readParam);

                fileJpg = new File(jpgPath);
                ImageIO.write(image, "jpg", fileJpg);
            }catch (Exception e){
                log.error("Webp图片转换为jpg异常：", e);
            }
        }

        return fileJpg;
    }

    /**
     * 是否匹配
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        if("webp".equalsIgnoreCase(input)){
            return true;
        }else{
            return false;
        }
    }

}
