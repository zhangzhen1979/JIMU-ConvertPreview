package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p> @Title ExcelToHtmlUtil
 * <p> @Description excel转化为html工具类
 *
 * @author ACGkaka
 * @date 2020/4/11 2:41
 */
public class Excel2HtmlUtil {

    private static final Integer MAX_COLUMN = 500;

    /**
     * excel转html， 带格式
     *
     * @param excelFile    excel文件
     * @param htmlFilePath 转换后的html文件路径
     */
    public static void excel2html(File excelFile, String htmlFilePath) throws IOException {
        List<Map<String, String>> excelMapList = readExcelToHtml(excelFile, true);
        getHtml(excelFile, excelMapList, htmlFilePath);
    }

    /**
     * 包装成html
     */
    private static void getHtml(File excelFile, List<Map<String, String>> excelMapList, String path) throws IOException {
        // Files.newBufferedWriter(Paths.get(path));
        FileUtil.touch(path);
        String html = "<html>" +
                "   <head>" +
                "       <title>" +
                excelFile.getName() +
                "</title>" +
                "<style type=\"text/css\">" +
                "        * {" +
                "            margin: 0;" +
                "            padding: 0;" +
                "        }" +
                "" +
                "        #tag {" +
                "            overflow: hidden;" +
                "            background: #000;" +
                "            border: 1px solid #000;" +
                "        }" +
                "" +
                "        #tag li {" +
                "            list-style: none;" +
                "            float: left;" +
                "            margin-right: 0px;" +
                "            color: white;" +
                "            padding: 5px 20px;" +
                "            cursor: pointer;" +
                "        }" +
                "" +
                "        #tag .current {" +
                "            color: #000;" +
                "            background: #ccc;" +
                "        }" +
                "" +
                "        #tagContent div {" +
                "            border: 1px solid #000;" +
                "            border-top: none;" +
                "            height: 300px;" +
                "            display: none;" +
                "        }" +
                "    </style>" +
                "   </head>" +
                "   <body>" +
                // 导航
                excelMapList.stream().map(map -> map.get("sheetName")).collect(Collectors.joining("</li><li>",
                        "<ul id=\"tag\"><li class=\"current\">", "</li></ul>")) +
                // sheet内容
                excelMapList.stream().map(map -> map.get("content"))
                        .collect(Collectors.joining("</div><div>", "<div id=\"tagContent\"><div>" , "</div></div>")) +
                "<script>" +
                "        var tag = document.getElementById(\"tag\").children; " +
                "        var content = document.getElementById(\"tagContent\").children; " +
                "        content[0].style.display = \"block\"; " +
                "        var len = tag.length;" +
                "        for (var i = 0; i < len; i++) {" +
                "            tag[i].index = i;" +
                "            tag[i].onclick = function () {" +
                "                for (var n = 0; n < len; n++) {" +
                "                    tag[n].className = \"\";" +
                "                    content[n].style.display = \"none\";" +
                "                }" +
                "                tag[this.index].className = \"current\";" +
                "                content[this.index].style.display = \"block\";" +
                "            }" +
                "        }" +
                "</script>"+
                "   </body>" +
                "</html>";
        Files.write(Paths.get(path), html.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * excel转html入口
     */
    private static List<Map<String, String>> readExcelToHtml(File excelFile, boolean isWithStyle) {
        List<Map<String, String>> excelInfoMapList = null;
        try (
                // 文件流
                InputStream inputStream = new FileInputStream(excelFile)
        ) {
            // 创建工作簿
            Workbook workbook = WorkbookFactory.create(inputStream);
            // Excel类型
            if (workbook instanceof HSSFWorkbook) {
                // 2003
                HSSFWorkbook hssfWorkbook = (HSSFWorkbook) workbook;
                // 获取Excel信息
                excelInfoMapList = getExcelInfo(hssfWorkbook, isWithStyle);
            } else if (workbook instanceof XSSFWorkbook) {
                // 2007
                XSSFWorkbook xssfWorkbook = (XSSFWorkbook) workbook;
                // 获取Excel信息
                excelInfoMapList = getExcelInfo(xssfWorkbook, isWithStyle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return excelInfoMapList;
    }

    /**
     * 获取Excel信息
     */
    private static List<Map<String, String>> getExcelInfo(Workbook workbook, boolean isWithStyle) {
        List<Map<String, String>> htmlMapList = new ArrayList<>();
        // 获取所有sheet
        int sheets = workbook.getNumberOfSheets();
        // 用于计算公式
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        // 遍历sheets
        for (int sheetIndex = 0; sheetIndex < sheets; sheetIndex++) {
            // 用于保存sheet信息
            Map<String, String> sheetMap = new HashMap<>();
            // 获取sheet名
            String sheetName = workbook.getSheetName(sheetIndex);
            // 存储sheet名
            sheetMap.put("sheetName", sheetName);
            sheetMap.put("labelName", "table_" + sheetName);
            StringBuffer stringBuffer = new StringBuffer();
            // 获取第一个sheet信息
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            // 行数
            int lastRowNum = sheet.getLastRowNum();
            // 获取合并后的单元格行列坐标
            Map<String, String>[] map = getRowSpanColSpan(sheet);
            stringBuffer.append("<table style='border-collapse:collapse;' width='100%'>");
            Row row;
            Cell cell;
            for (int rowNum = sheet.getFirstRowNum(); rowNum <= lastRowNum; rowNum++) {
                row = sheet.getRow(rowNum);
                if (row == null) {
                    stringBuffer.append("<tr><td>&nbsp;</td></tr>");
                    continue;
                }
                stringBuffer.append("<tr>");
                // 列数
                short lastCellNum = row.getLastCellNum();
                for (int colNum = 0; colNum <= lastCellNum; colNum++) {
                    // 获取列
                    cell = row.getCell(colNum);
                    // 空白单元格
                    if (cell == null) {
                        stringBuffer.append("<td>&nbsp;</td>");
                        continue;
                    }
                    // 获取列值
                    String cellValue = getCellValue(cell, evaluator);
                    if (map[0].containsKey(rowNum + "," + colNum)) {
                        String point = map[0].get(rowNum + "," + colNum);
                        map[0].remove(rowNum + "," + colNum);
                        int bottomRow = Integer.parseInt(point.split(",")[0]);
                        int bottomCol = Integer.parseInt(point.split(",")[1]);
                        int rowSpan = bottomRow - rowNum + 1;
                        int colSpan = bottomCol - colNum + 1;
                        stringBuffer.append("<td rowspan= '").append(rowSpan).append("' colSpan= '").append(colSpan).append("' ");
                    } else if (map[1].containsKey(rowNum + "," + colNum)) {
                        map[1].remove(rowNum + "," + colNum);
                        continue;
                    } else {
                        stringBuffer.append("<td ");
                    }

                    // 判断是否包含样式
                    if (isWithStyle) {
                        // 处理单元格样式
                        dealExcelStyle(workbook, sheet, cell, stringBuffer, rowNum == lastRowNum || rowNum > MAX_COLUMN);
                    }

                    stringBuffer.append(">");
                    if (cellValue == null || "".equals(cellValue.trim())) {
                        stringBuffer.append(" &nbsp; ");
                    } else {
                        stringBuffer.append(cellValue.replace(String.valueOf((char) 160), "&nbsp;"));
                    }
                    stringBuffer.append("</td>");
                }
                stringBuffer.append("</tr>");
                if (rowNum > MAX_COLUMN) {
                    stringBuffer.append("<tr><td colspan= '500'>数据量太大，请下载Excel查看更多数据……</td></tr>");
                    break;
                }
            }
            stringBuffer.append("</table>");
            sheetMap.put("content", stringBuffer.toString());
            htmlMapList.add(sheetMap);
        }
        return htmlMapList;
    }

    /**
     * 获取列值
     */
    private static String getCellValue(Cell cell, FormulaEvaluator evaluator) {
        String result;
        switch (cell.getCellType()) {
            case NUMERIC: // 数字类型
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat simpleDateFormat;
                    // 时间
                    if (cell.getCellStyle().getDataFormat() == HSSFDataFormat.getBuiltinFormat("h:mm")) {
                        simpleDateFormat = new SimpleDateFormat("HH:mm");
                    } else {
                        // 日期
                        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    }
                    Date date = cell.getDateCellValue();
                    result = simpleDateFormat.format(date);
                } else if (cell.getCellStyle().getDataFormat() == 58) {
                    // 处理自定义日期格式：m月d日（通过判断单元格格式的id解决，id值为58）
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    double value = cell.getNumericCellValue();
                    Date date = DateUtil.getJavaDate(value);
                    result = simpleDateFormat.format(date);
                } else {
                    double value = cell.getNumericCellValue();
                    CellStyle cellStyle = cell.getCellStyle();
                    DecimalFormat decimalFormat = new DecimalFormat();
                    String temp = cellStyle.getDataFormatString();
                    // 单元格设置成常规
                    if (temp.equals("General")) {
                        decimalFormat.applyPattern("#");
                    }
                    result = decimalFormat.format(value);
                }
                break;
            case STRING: // 字符串
                result = cell.getStringCellValue();
                break;
            case FORMULA: // 公式
                CellValue cellVal = evaluator.evaluate(cell);
                result = (cellVal.getCellType() == CellType.NUMERIC) ? String.valueOf(cellVal.getNumberValue()) : cellVal.getStringValue();
                break;
            default:
                result = "";
                break;
        }
        return result;
    }

    /**
     * 合并单元格
     *
     * @return
     */
    private static Map<String, String>[] getRowSpanColSpan(Sheet sheet) {
        Map<String, String> map0 = new HashMap<>();
        Map<String, String> map1 = new HashMap<>();
        // 获取合并后的单元格数量
        int mergeNum = sheet.getNumMergedRegions();
        CellRangeAddress range;
        for (int i = 0; i < mergeNum; i++) {
            range = sheet.getMergedRegion(i);
            int topRow = range.getFirstRow();
            int topCol = range.getFirstColumn();
            int bottomRow = range.getLastRow();
            int bottomCol = range.getLastColumn();
            map0.put(topRow + "," + topCol, bottomRow + "," + bottomCol);
            int tempRow = topRow;
            while (tempRow <= bottomRow) {
                int tempCol = topCol;
                while (tempCol <= bottomCol) {
                    map1.put(tempRow + "," + tempCol, "");
                    tempCol++;
                }
                tempRow++;
            }
            map1.remove(topRow + "," + topCol);
        }
        return new Map[]{map0, map1};
    }

    private static final String[] borders = {"border-top:", "border-right:", "border-bottom:", "border-left:"};
    private static final String[] borderStyles = {"solid ", "solid ", "solid ", "solid ", "solid ", "solid ", "solid ", "solid ", "solid ", "solid", "solid", "solid", "solid", "solid"};

    /**
     * 处理单元格样式
     */
    private static void dealExcelStyle(Workbook wb, Sheet sheet, Cell cell, StringBuffer sb, boolean isLastRow) {
        CellStyle cellStyle = cell.getCellStyle();
        if (cellStyle != null) {
            HorizontalAlignment alignment = cellStyle.getAlignment();
            // 单元格内容的水平对齐方式
            sb.append("align='").append(convertAlignToHtml(alignment)).append("' ");
            VerticalAlignment verticalAlignment = cellStyle.getVerticalAlignment();
            // 单元格中内容的垂直排列方式
            sb.append("valign='").append(convertVerticalAlignToHtml(verticalAlignment)).append("' ");

            if (wb instanceof XSSFWorkbook) {

                XSSFFont xf = ((XSSFCellStyle) cellStyle).getFont();
                boolean isBold = xf.getBold();
                sb.append("style='");
                sb.append("white-space: nowrap; ");
                sb.append("font-weight:").append(isBold ? "bold" : "normal").append("; ");   // 字体加粗
                sb.append("font-size:").append(xf.getFontHeight() / 2).append("%; ");   // 字体大小

                XSSFColor xc = xf.getXSSFColor();
                if (xc != null && !"".equals(xc)) {
                    sb.append("color:#").append(xc.getARGBHex().substring(2)).append("; ");  // 字体颜色
                }

                XSSFColor bgColor = (XSSFColor) cellStyle.getFillForegroundColorColor();
                if (bgColor != null && !"".equals(bgColor)) {
                    sb.append("background-color:#").append(bgColor.getARGBHex().substring(2)).append("; ");  // 背景颜色
                }
                sb.append(getBorderStyle(0, cellStyle.getBorderTop(), ((XSSFCellStyle) cellStyle).getTopBorderXSSFColor()));
                sb.append(getBorderStyle(1, cellStyle.getBorderRight(), ((XSSFCellStyle) cellStyle).getRightBorderXSSFColor()));
                sb.append(getBorderStyle(3, cellStyle.getBorderLeft(), ((XSSFCellStyle) cellStyle).getLeftBorderXSSFColor()));
                sb.append(getBorderStyle(2, cellStyle.getBorderBottom(), ((XSSFCellStyle) cellStyle).getBottomBorderXSSFColor()));

            } else if (wb instanceof HSSFWorkbook) {

                HSSFFont hf = ((HSSFCellStyle) cellStyle).getFont(wb);
                boolean isBold = hf.getBold();
                short fontColor = hf.getColor();
                sb.append("style='");
                HSSFPalette palette = ((HSSFWorkbook) wb).getCustomPalette(); // 类HSSFPalette用于求的颜色的国际标准形式
                HSSFColor hc = palette.getColor(fontColor);
                sb.append("font-weight:").append(isBold ? "bold" : "normal").append(";"); // 字体加粗
                sb.append("font-size: ").append(hf.getFontHeight() / 2).append("%;"); // 字体大小
                String fontColorStr = convertToStardColor(hc);
                if (fontColorStr != null && !"".equals(fontColorStr.trim())) {
                    sb.append("color:").append(fontColorStr).append(";"); // 字体颜色
                }
                short bgColor = cellStyle.getFillForegroundColor();
                hc = palette.getColor(bgColor);
                String bgColorStr = convertToStardColor(hc);
                if (bgColorStr != null && !"".equals(bgColorStr.trim())) {
                    sb.append("background-color:").append(bgColorStr).append(";"); // 背景颜色
                }
                sb.append(getBorderStyle(palette, 0, cellStyle.getBorderTop(), cellStyle.getTopBorderColor()));
                sb.append(getBorderStyle(palette, 1, cellStyle.getBorderRight(), cellStyle.getRightBorderColor()));
                sb.append(getBorderStyle(palette, 3, cellStyle.getBorderLeft(), cellStyle.getLeftBorderColor()));
                sb.append(getBorderStyle(palette, 2, cellStyle.getBorderBottom(), cellStyle.getBottomBorderColor()));
            }

            sb.append("' ");
        }
    }

    /**
     * 垂直对齐方式
     *
     * @param verticalAlignment
     * @return
     */
    private static String convertVerticalAlignToHtml(VerticalAlignment verticalAlignment) {
        String align = "middle";
        switch (verticalAlignment) {
            case BOTTOM:
                align = "bottom";
                break;
            case CENTER:
                align = "center";
                break;
            case TOP:
                align = "top";
                break;
            default:
                break;
        }
        return align;
    }

    /**
     * 水平对齐方式
     */
    private static String convertAlignToHtml(HorizontalAlignment alignment) {
        String align = "left";
        switch (alignment) {
            case LEFT:
                align = "left";
                break;
            case CENTER:
                align = "center";
                break;
            case RIGHT:
                align = "right";
                break;
            default:
                break;
        }
        return align;
    }

    private static String getBorderStyle(int b, BorderStyle s, XSSFColor xc) {
        if (s == BorderStyle.NONE) {
            return borders[b] + borderStyles[s.getCode()] + "#d0d7e5 1px;";
        }
        if (xc != null) {
            String borderColorStr = xc.getARGBHex();
            borderColorStr = borderColorStr == null || borderColorStr.length() < 1 ? "#000000" : borderColorStr.substring(2);
            return borders[b] + borderStyles[s.getCode()] + borderColorStr + " 1px;";
        }

        return "";
    }

    private static String getBorderStyle(HSSFPalette palette, int b, BorderStyle s, short t) {
        if (s == BorderStyle.NONE) {
            return borders[b] + borderStyles[s.getCode()] + "#d0d7e5 1px;";
        }
        String borderColorStr = convertToStardColor(palette.getColor(t));
        assert borderColorStr != null;
        borderColorStr = borderColorStr.length() < 1 ? "#000000" : borderColorStr;
        return borders[b] + borderStyles[s.getCode()] + borderColorStr + " 1px;";

    }

    private static String convertToStardColor(HSSFColor hc) {
        StringBuilder sb = new StringBuilder();
        if (hc != null) {
            if (HSSFColor.HSSFColorPredefined.AUTOMATIC.getIndex() == hc.getIndex()) {
                return null;
            }
            sb.append("#");
            for (int i = 0; i < hc.getTriplet().length; i++) {
                sb.append(fillWithZero(Integer.toHexString(hc.getTriplet()[i])));
            }
        }
        return sb.toString();
    }

    private static String fillWithZero(String str) {
        if (str != null && str.length() < 2) {
            return "0" + str;
        }
        return str;
    }
}
