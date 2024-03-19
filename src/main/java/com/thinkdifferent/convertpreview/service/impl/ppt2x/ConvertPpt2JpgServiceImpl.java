package com.thinkdifferent.convertpreview.service.impl.ppt2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author ltian
 * @version 1.0
 * @date 2024/1/22 19:43
 */
@Log4j2
@Service
@ConditionalOnProperty(name = "convert.preview.poi.ppt", havingValue = "true")
public class ConvertPpt2JpgServiceImpl implements ConvertTypeService {
    /**
     * 根据类型进行转换, 内部实现，不建议直接调用
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        FileUtil.mkdir(targetDir);
        if (inputFile.getName().endsWith(".pptx") || inputFile.getName().endsWith(".PPTX")) {
            pptx2Image(inputFile, targetDir);
        } else {
            ppt2Image(inputFile, targetDir);
        }

        return new File(targetDir);
    }

    private static final int zoom = 4;

    /**
     * 将pptx转成图片,保存在同一目录的image目录下
     *
     * @param filePptx      pptx文件
     * @param strTargetPath 图片父目录
     * @throws IOException
     */
    public static void pptx2Image(File filePptx, String strTargetPath) throws Exception {
        FileInputStream fis = new FileInputStream(filePptx);
        // 如果是pptx文件
        XMLSlideShow pptx = new XMLSlideShow(fis);

        // 获取每一页幻灯片
        List<XSLFSlide> slides = pptx.getSlides();
        for (int i = 0; i < slides.size(); i++) {
            // 获取单个幻灯片
            XSLFSlide slide = slides.get(i);
            // 获取幻灯片大小
            Dimension pgsize = pptx.getPageSize();

            int width = (int) pgsize.getWidth();
            int height = (int) pgsize.getHeight();

            // 创建BufferedImage对象，用于保存图片
            BufferedImage img = new BufferedImage(width * zoom, height * zoom, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = img.createGraphics();
            graphics.setPaint(Color.white);
            graphics.scale(zoom, zoom);
            graphics.fill(new Rectangle2D.Float(0, 0, width * zoom, height * zoom));

            // 绘制幻灯片
            slide.draw(graphics);

            // 保存图片
            File imageFile = new File(strTargetPath + "/" + (i + 1) + ".jpg");
            FileOutputStream out = new FileOutputStream(imageFile);
            ImageIO.write(img, "jpg", out);
            IOUtils.closeQuietly(out);
        }
        IOUtils.closeQuietly(fis);
    }

    /**
     * 将ppt转成图片,保存在同一目录的image目录下
     *
     * @param filePpt       ppt文件
     * @param strTargetPath 图片父目录
     * @throws IOException err
     */
    public static void ppt2Image(File filePpt, String strTargetPath) throws Exception {
        FileInputStream fis = new FileInputStream(filePpt);
        // 如果是ppt文件
        HSLFSlideShow ppt = new HSLFSlideShow(fis);

        // 获取每一页幻灯片
        List<HSLFSlide> slides = ppt.getSlides();
        for (int i = 0; i < slides.size(); i++) {
            // 获取单个幻灯片
            HSLFSlide slide = slides.get(i);
            // 获取幻灯片大小
            Dimension pgsize = ppt.getPageSize();

            int width = (int) pgsize.getWidth();
            int height = (int) pgsize.getHeight();

            // 创建BufferedImage对象，用于保存图片
            BufferedImage img = new BufferedImage(width * zoom, height * zoom, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = img.createGraphics();
            graphics.setPaint(Color.white);
            graphics.scale(zoom, zoom);
            graphics.fill(new Rectangle2D.Float(0, 0, width * zoom, height * zoom));

            // 绘制幻灯片
            slide.draw(graphics);

            // 保存图片
            File imageFile = new File(strTargetPath + "/" + (i + 1) + ".jpg");
            FileOutputStream out = new FileOutputStream(imageFile);
            ImageIO.write(img, "jpg", out);
            IOUtils.closeQuietly(out);
        }
        IOUtils.closeQuietly(fis);

    }
}
