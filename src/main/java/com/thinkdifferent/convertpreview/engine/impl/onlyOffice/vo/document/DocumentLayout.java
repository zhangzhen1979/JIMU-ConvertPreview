package com.thinkdifferent.convertpreview.engine.impl.onlyOffice.vo.document;

import lombok.Data;

/**
 * @BelongsProject: leaf-onlyoffice
 * @BelongsPackage: com.ideayp.leaf.onlyoffice.dto.convert
 * @Author: TongHui
 * @CreateTime: 2022-11-23 09:44
 * @Description: 文档布局
 * @Version: 1.0
 */
@Data
public class DocumentLayout {
    /**定义是否绘制占位符。*/
    private  Boolean drawPlaceHolders;
    /**定义是否突出显示表单。*/
    private  Boolean drawFormHighlight;
    /***
     * 定义打印模式是打开还是关闭。
     * 此参数仅用于将docx/docxf转换为pdf。
     * 如果此参数等于true，则如上所述使用drawPlaceHolder和drawFormHighlight标志。
     * 如果此参数为false，则drawFormHighlight标志不起作用，
     * 并且drawPlaceHolder参数允许以pdf格式保存表单。 默认值为false。
     */
    private  Boolean isPrint;
}
