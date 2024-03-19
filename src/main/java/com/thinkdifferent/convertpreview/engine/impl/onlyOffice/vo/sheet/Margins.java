package com.thinkdifferent.convertpreview.engine.impl.onlyOffice.vo.sheet;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @BelongsProject: leaf-onlyoffice
 * @BelongsPackage: com.ideayp.leaf.onlyoffice.dto.convert.sheet
 * @Author: TongHui
 * @CreateTime: 2022-11-23 09:55
 * @Description: 边距
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
public class Margins {
    /**设置输出 PDF 文件的边距 默认 19.1mm */
    private String bottom;
    /** 默认 17.8mm */
    private String left;
    /** 默认 17.8mm */
    private String right;
    /** 默认 19.1mm */
    private String top;
}
