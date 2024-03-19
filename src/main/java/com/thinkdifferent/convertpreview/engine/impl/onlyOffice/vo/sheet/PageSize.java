package com.thinkdifferent.convertpreview.engine.impl.onlyOffice.vo.sheet;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @BelongsProject: leaf-onlyoffice
 * @BelongsPackage: com.ideayp.leaf.onlyoffice.dto.convert.sheet
 * @Author: TongHui
 * @CreateTime: 2022-11-23 09:58
 * @Description: 页面大小
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
public class PageSize {
    /** 设置输出 PDF 文件的页面高度。默认 297mm*/
    private String height;
    /** 设置输出 PDF 文件的页面宽度。默认 210mm*/
    private String width;
}
