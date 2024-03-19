package com.thinkdifferent.convertpreview.utils.poi;

/**
 * @author ltian
 * @version 1.0
 * @date 2024/1/17 16:55
 */

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Formatter;

public class XSSFHtmlHelper
        implements HtmlHelper {
    private final XSSFWorkbook wb;

    public XSSFHtmlHelper(XSSFWorkbook wb) {
        this.wb = wb;
    }

    public void colorStyles(CellStyle style, Formatter out) {
        XSSFCellStyle cs = (XSSFCellStyle) style;
        styleColor(out, "background-color", cs.getFillForegroundXSSFColor());
        styleColor(out, "text-color", cs.getFont().getXSSFColor());
    }

    private void styleColor(Formatter out, String attr, XSSFColor color) {
        if ((color == null) || (color.isAuto())) {
            return;
        }
        byte[] rgb = color.getRGB();
        if (rgb == null) {
            return;
        }

        out.format("  %s: #%02x%02x%02x;%n", new Object[]{attr, Byte.valueOf(rgb[0]), Byte.valueOf(rgb[1]), Byte.valueOf(rgb[2])});
    }
}
