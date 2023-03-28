/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Copyright Tim Hutton and Berlin Brown <berlin dot brown at gmail.com> 2011
 * <p>
 * Tim Hutton is the original author, but a license not provided in source,
 * GPL was used for similar projects.  If Tim or anyone else has questions, please contact Berlin Brown.
 * <p>
 * http://www.sq3.org.uk/Evolution/Squirm3/
 */
package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import lombok.Cleanup;
import org.apache.commons.lang3.StringUtils;
import org.xhtmlrenderer.swing.Java2DRenderer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Convert XHTML output to image using xhtmlrenderer
 * <p>
 * See:
 * http://flyingsaucerproject.github.com/flyingsaucer/r8/guide/users-guide-R8.html#xil_29
 *
 * @author berlin
 */
public class XHTMLToImage {

    /**
     * Convert XHTML output to image.
     */
    public static File convertToImage(String inputFilename, String strHtml,
                                      String outputFilename,
                                      int widthImage, int heightImage,
                                      Map<String, String> mapData) throws IOException {
        if (StringUtils.isEmpty(strHtml)) {
            // Generate an image from a file:
            File fileTemplate = new File(inputFilename);
            if(fileTemplate.exists()){
                // 文件转String
                strHtml = file2String(fileTemplate, "UTF-8");
                // 替换变量
                for (String key : mapData.keySet()) {
                    strHtml = strHtml.replaceAll("#" + key, mapData.get(key));
                }
            }else{
                return null;
            }
        }

        // String转File
        String uuid = UUID.randomUUID().toString();
        outputFilename = outputFilename.replaceAll("\\\\", "/");
        String path = outputFilename.substring(0, outputFilename.lastIndexOf("/") + 1);
        File f = string2File(strHtml, path + uuid + ".html");

        // can specify width alone, or width + height
        // constructing does not render; not until getImage() is called
        Java2DRenderer renderer = new Java2DRenderer(f, widthImage, heightImage);
        // this renders and returns the image, which is stored in the J2R; will not
        // be re-rendered, calls to getImage() return the same instance
        BufferedImage img = renderer.getImage();

        File filePng = new File(outputFilename);

        // 输出png图片
        ImageIO.write(img, "png", filePng);

        if (transferAlpha2File(img, outputFilename)) {
            // 删除临时文件
            FileUtil.del(f);
            return filePng;
        } else {
            // 删除临时文件
            FileUtil.del(f);
        }

        return null;
    }

    public static boolean transferAlpha2File(BufferedImage imgInput, String imgTarget) throws IOException {
        ImageIcon imageIcon = new ImageIcon(imgInput);
        BufferedImage bufferedImage = new BufferedImage(imageIcon.getIconWidth(), imageIcon.getIconHeight(),
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2D = (Graphics2D) bufferedImage.getGraphics();
        g2D.drawImage(imageIcon.getImage(), 0, 0, imageIcon.getImageObserver());
        int alpha = 0;
        for (int j1 = bufferedImage.getMinY(); j1 < bufferedImage.getHeight(); j1++) {
            for (int j2 = bufferedImage.getMinX(); j2 < bufferedImage.getWidth(); j2++) {
                int rgb = bufferedImage.getRGB(j2, j1);
                int R = (rgb & 0xff0000) >> 16;
                int G = (rgb & 0xff00) >> 8;
                int B = (rgb & 0xff);
                if (((255 - R) < 30) && ((255 - G) < 30) && ((255 - B) < 30)) {
                    rgb = ((alpha + 1) << 24) | (rgb & 0x00ffffff);
                }
                bufferedImage.setRGB(j2, j1, rgb);
            }
        }
        g2D.drawImage(bufferedImage, 0, 0, imageIcon.getImageObserver());

        File fileTarget = new File(imgTarget);
        boolean blnPng = ImageIO.write(bufferedImage, "png", fileTarget);

        return blnPng;// 直接输出文件
    }

    public static void main(final String[] args) throws Exception {
        final XHTMLToImage renderer = new XHTMLToImage();
        String inputFilename;
        String outputFilename;
        int widthImage = 500;
        int heightImage = 200;

        if (args.length != 2) {
            inputFilename = "d:/gdz.html";
            outputFilename = "d:/gdz.png";
        } else {
            inputFilename = args[0];
            outputFilename = args[1];
        }

        if (args.length == 4) {
            widthImage = Integer.parseInt("600");
            heightImage = Integer.parseInt("200");
        }


        Map<String, String> mapData = new HashMap<>();
        mapData.put("fonds_no", "1001");
        mapData.put("retention", "长期");
        mapData.put("year", "2022");
        mapData.put("piece_no", "45");

        renderer.convertToImage(inputFilename, "",
                outputFilename,
                widthImage, heightImage,
                mapData);
    }

    /**
     * 文本文件转换为指定编码的字符串
     *
     * @param file     文本文件
     * @param encoding 编码类型（UTF-8、GBK）
     * @return 转化后的字符串
     */
    private static String file2String(File file, String encoding) throws IOException {
        @Cleanup StringWriter writer = new StringWriter();
        @Cleanup InputStreamReader reader = (encoding == null || "".equals(encoding.trim())) ?
                new InputStreamReader(new FileInputStream(file), encoding) :
                new InputStreamReader(new FileInputStream(file));
        char[] buffer = new char[2048];
        int n;
        while (-1 != (n = reader.read(buffer))) {
            writer.write(buffer, 0, n);
        }

        if (writer != null) {
            return writer.toString();
        } else {
            return null;
        }
    }

    /**
     * 将String字符串转换为文件
     *
     * @param inputStr 输入的字符串
     * @param destFile 目标文件路径和文件名
     * @return 文件对象
     */
    private static File string2File(String inputStr, String destFile) {
        File fileDest = new File(destFile);
        FileUtil.writeString(inputStr, fileDest, "utf-8");

        return fileDest;
    }

} // End of the class //
