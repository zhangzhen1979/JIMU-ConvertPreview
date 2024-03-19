package com.thinkdifferent.convertpreview.config;

import com.thinkdifferent.convertpreview.utils.SystemUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigEngineBase {

    /**
     * 可转换的图片格式
     */
    public static String picType;
    @Value("${convert.engine.picType:tif,tiff,png,jpg,jpeg,bmp,psd,sgi,pcx,webp,batik,icns,pnm,pict,tga,iff,hdr,gif}")
    public void setPicType(String picType) {
        ConvertDocConfigEngineBase.picType = picType;
    }

    /**
     * 可直接预览的其他文件格式扩展名
     */
    public static String otherFileType;
    @Value("${convert.engine.otherFileType:}")
    public void setOtherFileType(String otherFileType) {
        ConvertDocConfigEngineBase.otherFileType = otherFileType;
    }
    /**
     * CAD转换工具路径
     */
    public static String cadUtilPath;
    @Value("${convert.engine.otherUtilPath.cad:}")
    public void setCadUtilPath(String cadUtilPath) {
        ConvertDocConfigEngineBase.cadUtilPath = SystemUtil.getPath(cadUtilPath);
    }


    /**
     * 自动加入页码水印的默认设置。是否开启。默认：否
     */
    public static Boolean autoAddPageNumEnabled;
    @Value("${convert.engine.autoAddPageNum.enabled:false}")
    public void setAutoAddPageNum(Boolean autoAddPageNumEnabled) {
        ConvertDocConfigEngineBase.autoAddPageNumEnabled = autoAddPageNumEnabled;
    }
    /**
     * 文件的处理类型：single：单个文件；multi：多个文件合并（默认值）；all：以上两种类型
     */
    public static String autoAddPageNumType;
    @Value("${convert.engine.autoAddPageNum.type:multi}")
    public void setAutoAddPageNumType(String autoAddPageNumType) {
        ConvertDocConfigEngineBase.autoAddPageNumType = autoAddPageNumType;
    }

    /**
     * 页码字号。默认：15
     */
    public static Float autoAddPageNumFontSize;
    @Value("${convert.engine.autoAddPageNum.fontSize:15}")
    public void setAutoAddPageNumFontSize(Float autoAddPageNumFontSize) {
        ConvertDocConfigEngineBase.autoAddPageNumFontSize = autoAddPageNumFontSize;
    }
    /**
     * 页码文字颜色。默认：黑色
     */
    public static String autoAddPageNumFontColor;
    @Value("${convert.engine.autoAddPageNum.fontColor:black}")
    public void setAutoAddPageNumFontColor(String autoAddPageNumFontColor) {
        ConvertDocConfigEngineBase.autoAddPageNumFontColor = autoAddPageNumFontColor;
    }

    /**
     * 页码位置。默认：顶部靠右
     */
    public static String autoAddPageNumLocate;
    @Value("${convert.engine.autoAddPageNum.locate:TR}")
    public void setAutoAddPageNumLocate(String autoAddPageNumLocate) {
        ConvertDocConfigEngineBase.autoAddPageNumLocate = autoAddPageNumLocate;
    }

    /**
     * 页码边距
     */
    public static int autoAddPageNumMargins;
    @Value("${convert.engine.autoAddPageNum.margins:30}")
    public void setAutoAddPageNumPdfMargins(int autoAddPageNumMargins) {
        ConvertDocConfigEngineBase.autoAddPageNumMargins = autoAddPageNumMargins;
    }

    /**
     * 页码奇偶页交换位置。默认：否
     */
    public static Boolean autoAddPageNumSwapPosition;
    @Value("${convert.engine.autoAddPageNum.swapPosition:false}")
    public void setAutoAddPageNumSwapPosition(Boolean autoAddPageNumSwapPosition) {
        ConvertDocConfigEngineBase.autoAddPageNumSwapPosition = autoAddPageNumSwapPosition;
    }

    /**
     * 自动补零位数。如果不为0，则自动补0；如果为0，则不补0。
     */
    public static int autoAddPageNumDigits;
    @Value("${convert.engine.autoAddPageNum.digits:0}")
    public void setAutoAddPageNumDigits(int autoAddPageNumDigits) {
        ConvertDocConfigEngineBase.autoAddPageNumDigits = autoAddPageNumDigits;
    }


    /**
     * 自动加入【版权声明】水印的默认设置。是否开启。默认：否
     */
    public static Boolean autoAddCopyRightEnabled;
    @Value("${convert.engine.autoAddCopyRight.enabled:false}")
    public void setAutoAddCopyRight(Boolean autoAddCopyRightEnabled) {
        ConvertDocConfigEngineBase.autoAddCopyRightEnabled = autoAddCopyRightEnabled;
    }
    /**
     * 文件的处理类型：single：单个文件；multi：多个文件合并（默认值）；all：以上两种类型
     */
    public static String autoAddCopyRightType;
    @Value("${convert.engine.autoAddCopyRight.type:multi}")
    public void setAutoAddCopyRightType(String autoAddCopyRightType) {
        ConvertDocConfigEngineBase.autoAddCopyRightType = autoAddCopyRightType;
    }

    /**
     * 版权声明文字：默认：CopyRight
     */
    public static String autoAddCopyRightText;
    @Value("${convert.engine.autoAddCopyRight.text:CopyRight}")
    public void setAutoAddCopyRightText(String autoAddCopyRightText) {
        ConvertDocConfigEngineBase.autoAddCopyRightText = autoAddCopyRightText;
    }
    /**
     * 页码字号。默认：15
     */
    public static Float autoAddCopyRightFontSize;
    @Value("${convert.engine.autoAddCopyRight.fontSize:15}")
    public void setAutoAddCopyRightFontSize(Float autoAddCopyRightFontSize) {
        ConvertDocConfigEngineBase.autoAddCopyRightFontSize = autoAddCopyRightFontSize;
    }
    /**
     * 页码文字颜色。默认：黑色
     */
    public static String autoAddCopyRightFontColor;
    @Value("${convert.engine.autoAddCopyRight.fontColor:black}")
    public void setAutoAddCopyRightFontColor(String autoAddCopyRightFontColor) {
        ConvertDocConfigEngineBase.autoAddCopyRightFontColor = autoAddCopyRightFontColor;
    }

    /**
     * 页码位置。默认：顶部靠右
     */
    public static String autoAddCopyRightLocate;
    @Value("${convert.engine.autoAddCopyRight.locate:TR}")
    public void setAutoAddCopyRightLocate(String autoAddCopyRightLocate) {
        ConvertDocConfigEngineBase.autoAddCopyRightLocate = autoAddCopyRightLocate;
    }

    /**
     * 页码边距
     */
    public static int autoAddCopyRightMargins;
    @Value("${convert.engine.autoAddCopyRight.margins:30}")
    public void setAutoAddCopyRightPdfMargins(int autoAddCopyRightMargins) {
        ConvertDocConfigEngineBase.autoAddCopyRightMargins = autoAddCopyRightMargins;
    }

    /**
     * 页码奇偶页交换位置。默认：否
     */
    public static Boolean autoAddCopyRightSwapPosition;
    @Value("${convert.engine.autoAddCopyRight.swapPosition:false}")
    public void setAutoAddCopyRightSwapPosition(Boolean autoAddCopyRightSwapPosition) {
        ConvertDocConfigEngineBase.autoAddCopyRightSwapPosition = autoAddCopyRightSwapPosition;
    }

}
