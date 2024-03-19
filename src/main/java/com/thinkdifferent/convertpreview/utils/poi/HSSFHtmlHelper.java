package com.thinkdifferent.convertpreview.utils.poi;

/**
 * @author ltian
 * @version 1.0
 * @date 2024/1/17 16:54
 */

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;

import java.util.Formatter;

public class HSSFHtmlHelper
        implements HtmlHelper {
    private final HSSFWorkbook wb;
    private final HSSFPalette colors;
    private static final HSSFColor HSSF_AUTO = new HSSFColor();

    public HSSFHtmlHelper(HSSFWorkbook wb) {
        this.wb = wb;

        this.colors = wb.getCustomPalette();
    }

    public void colorStyles(CellStyle style, Formatter out) {
        HSSFCellStyle cs = (HSSFCellStyle) style;
        out.format("  /* fill pattern = %d */%n", cs.getFillPattern().getCode());
        styleColor(out, "background-color", cs.getFillForegroundColor());
        styleColor(out, "color", cs.getFont(this.wb).getColor());
        styleColor(out, "border-left-color", cs.getLeftBorderColor());
        styleColor(out, "border-right-color", cs.getRightBorderColor());
        styleColor(out, "border-top-color", cs.getTopBorderColor());
        styleColor(out, "border-bottom-color", cs.getBottomBorderColor());
    }

    private void styleColor(Formatter out, String attr, short index) {
        HSSFColor color = this.colors.getColor(index);
        if ((index == HSSF_AUTO.getIndex()) || (color == null)) {
            out.format("  /* %s: index = %d */%n", new Object[]{attr, Short.valueOf(index)});
        } else {
            short[] rgb = color.getTriplet();
            out.format("  %s: #%02x%02x%02x; /* index = %d */%n", new Object[]{attr, Short.valueOf(rgb[0]),
                    Short.valueOf(rgb[1]), Short.valueOf(rgb[2]), Short.valueOf(index)});
        }
    }
}
