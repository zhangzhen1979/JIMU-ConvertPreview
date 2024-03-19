package com.thinkdifferent.convertpreview.config;

import com.thinkdifferent.convertpreview.utils.SystemUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigBase {

    /**
     * 当前服务器【内网】访问地址。
     */
    public static String baseUrl;
    @Value("${convert.baseUrl:}")
    public void setBaseUrl(String baseUrl) {
        ConvertDocConfigBase.baseUrl = baseUrl;
    }
    /**
     * URLEncode编码使用的key。
     */
    public static String urlEncodeKey;
    @Value("${convert.urlEncodeKey:archivesAes14023}")
    public void setUrlEncodeKey(String urlEncodeKey) {
        ConvertDocConfigBase.urlEncodeKey = urlEncodeKey;
    }

    /**
     * 输入文件的临时路径
     */
    public static String inPutTempPath;
    @Value("${convert.path.inPutTempPath:intemp/}")
    public void setInPutTempPath(String inPutTempPath) {
        inPutTempPath = SystemUtil.getPath(inPutTempPath);

        if (!inPutTempPath.endsWith("intemp/")) {
            inPutTempPath = inPutTempPath + "intemp/";
        }
        ConvertDocConfigBase.inPutTempPath = inPutTempPath;
    }

    /**
     * 输出文件的临时路径
     */
    public static String outPutPath;
    @Value("${convert.path.outPutPath:outtemp/}")
    public void setOutPutPath(String outPutPath) {
        outPutPath = SystemUtil.getPath(outPutPath);

        if (!outPutPath.endsWith("outtemp/")) {
            outPutPath = outPutPath + "outtemp/";
        }
        ConvertDocConfigBase.outPutPath = SystemUtil.beautifulPath(outPutPath);
    }

    /**
     * 静态文件解析路径
     */
    public static String staticLocations;
    @Value("${spring.resources.static-locations:}")
    public void setStaticLocations(String staticLocations){
        ConvertDocConfigBase.staticLocations = staticLocations;
    }
    /**
     * url路径
     */
    public static String staticPathPattern;
    @Value("${spring.mvc.static-path-pattern:}")
    public void setStaticPathPattern(String staticPathPattern){
        ConvertDocConfigBase.staticPathPattern = staticPathPattern;
    }
    /**
     * 解析文件的扩展名
     */
    public static String suffix;
    @Value("${spring.mvc.view.suffix:}")
    public void setSuffix(String suffix){
        ConvertDocConfigBase.suffix = suffix;
    }



    /**
     * 水印字体文件
     */
    public static String waterMarkFont;
    @Value("${convert.pdf.font:}")
    public void setWaterMarkFont(String waterMarkFont) {
        if(!StringUtils.isEmpty(waterMarkFont)){
            ConvertDocConfigBase.waterMarkFont = SystemUtil.getPath(waterMarkFont);
        }
    }
    /**
     * 水印字体名称
     */
    public static String waterMarkFontName;
    @Value("${convert.pdf.fontName:}")
    public void setWaterMarkFontName(String waterMarkFontName) {
        ConvertDocConfigBase.waterMarkFontName = waterMarkFontName;
    }
    /**
     * 水印字体FamilyName
     */
    public static String waterMarkFontFamilyName;
    @Value("${convert.pdf.fontFamilyName:}")
    public void setWaterMarkFontFamilyName(String waterMarkFontFamilyName) {
        ConvertDocConfigBase.waterMarkFontFamilyName = waterMarkFontFamilyName;
    }

    /**
     * 文本型PDF转图片型pdf，生成的图片的分辨率（dpi）。默认值：100
     */
    public static int picDpi;
    @Value("${convert.pdf.picDpi:100}")
    public void setPicDpi(int picDpi) {
        ConvertDocConfigBase.picDpi = picDpi;
    }

    /**
     * 图片文件合成pdf时，可以设置合成pdf后图片的压缩率。默认值为0.3。1为不压缩，0.5为压缩50%
     */
    public static double picQuality;
    @Value("${convert.pdf.picQuality:0.3}")
    public void setPicQuality(double picQuality) {
        ConvertDocConfigBase.picQuality = picQuality;
    }

    /**
     * 重试次数, 默认不开启重试
     */
    public static int maxRetryNum;
    @Value("${convert.retry.max:0}")
    public void setMaxRetryNum(int maxRetryNum) {
        ConvertDocConfigBase.maxRetryNum = maxRetryNum;
    }

}
