package com.thinkdifferent.convertpreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConvertDocConfigEnginePOI {


    /**
     * 是否启用内置POI完成Word的预览、转换。默认值：true
     */
    public static Boolean poiWord;

    @Value("${convert.preview.poi.word:true}")
    public void setPoiWord(Boolean poiWord) {
        ConvertDocConfigEnginePOI.poiWord = poiWord;
    }

    /**
     * 是否启用内置POI完成Excel的预览、转换。默认值：true
     */
    public static Boolean poiExcel;

    @Value("${convert.preview.poi.excel:true}")
    public void setPoiExcel(Boolean poiExcel) {
        ConvertDocConfigEnginePOI.poiExcel = poiExcel;
    }

    /**
     * 是否启用内置POI完成PPT的预览、转换。默认值：true
     */
    public static Boolean poiPpt;

    @Value("${convert.preview.poi.ppt:true}")
    public void setPoiPpt(Boolean poiPpt) {
        ConvertDocConfigEnginePOI.poiPpt = poiPpt;
    }

    /**
     * 文本类型文件扩展名
     */
    public static String txtExt;

    @Value("${convert.preview.txtExt:}")
    public void setTxtExt(String txtExt) {
        ConvertDocConfigEnginePOI.txtExt = txtExt;
    }

}
