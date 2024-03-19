package com.thinkdifferent.convertpreview.engine.impl.onlyOffice.vo.sheet;

import lombok.Data;

/**
 * @BelongsProject: leaf-onlyoffice
 * @BelongsPackage: com.ideayp.leaf.onlyoffice.dto.convert
 * @Author: TongHui
 * @CreateTime: 2022-11-23 09:50
 * @Description: 电子表格布局
 * @Version: 1.0
 */
@Data
public class SpreadsheetLayout {
    /**设置转换区域的高度（以页数为单位）。 默认值为0。*/
    private Integer fitToHeight;
    /**设置转换区域的宽度（以页数为单位）。 默认值为0。*/
    private Integer fitToWidth;
    /**允许是否在输出PDF文件中包含网格线。 默认值为false。*/
    private Boolean gridLines;
    /**允许是否包含输出PDF文件的标题。 默认值为false。*/
    private Boolean headings;
    /**确定是否忽略为电子表格文件选择的打印区域。 默认值为true。*/
    private Boolean ignorePrintArea;

    private Margins margins;
    /**设置输出 PDF 文件的方向。 可能是landscape, portrait.  默认值为纵向(portrait)。*/
    private String orientation;

    private PageSize pageSize;
    /**允许设置输出PDF文件的比例。 默认值为100。*/
    private Integer scale;
}
