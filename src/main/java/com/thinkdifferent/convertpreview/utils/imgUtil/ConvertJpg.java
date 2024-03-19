package com.thinkdifferent.convertpreview.utils.imgUtil;

import java.io.IOException;
import java.util.List;

public abstract class ConvertJpg {

    /**
     * 图片 转  JPG。
     * 支持输入格式如下：BMP、GIF、FlashPix、JPEG、PNG、PMN、TIFF、WBMP；WEBP
     *
     * @param strInputFile  输入文件的路径和文件名
     * @param strOutputFile 输出文件的路径和文件名
     * @return              转换后JPG文件路径（String）的List对象
     */
    public abstract List<String> convert(String strInputFile, String strOutputFile)
            throws IOException ;

    /**
     * 是否匹配
     * @param input 输入内容
     * @return 是否匹配
     */
    public abstract boolean match(String input);

}
