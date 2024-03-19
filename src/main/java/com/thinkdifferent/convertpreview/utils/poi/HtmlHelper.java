package com.thinkdifferent.convertpreview.utils.poi;

import org.apache.poi.ss.usermodel.CellStyle;

import java.util.Formatter;

/**
 * @author ltian
 * @version 1.0
 * @date 2024/1/17 16:51
 */
public abstract interface HtmlHelper {
    public abstract void colorStyles(CellStyle paramCellStyle, Formatter paramFormatter);
}
