package com.thinkdifferent.convertpreview.utils.poi;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.monitorjbl.xlsx.StreamingReader;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.service.impl.engine.ConvertEngine2PdfServiceImpl;
import com.thinkdifferent.convertpreview.utils.AesUtil;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.format.CellFormatResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;
import org.springframework.web.util.HtmlUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.poi.hssf.record.cf.BorderFormatting.*;

/**
 * <p> @Title ExcelToHtmlUtil
 * <p> @Description excel转化为html工具类
 *
 * @author ACGkaka
 * @date 2020/4/11 2:41
 */
@Log4j2
public class Excel2HtmlUtil {

    /**
     * excel转html， 带格式
     *
     * @param excelFile    excel文件
     * @param htmlFilePath 转换后的html文件路径, /usr/home/xxx/xxx.html
     */
    public static File excel2html(File excelFile, String htmlFilePath) throws Exception {
        String imgPath = htmlFilePath + "_html_imgs";

        // 判断xlsx 行列是否满足解析条件
        if (excelFile.getName().endsWith(".xls") || checkXlsxStream(excelFile)) {
            try (FileInputStream in = new FileInputStream(excelFile)) {
                return ToHtml.excelToHtml(in, htmlFilePath, imgPath);
            }
        }

        // 不满足条件的，尝试使用引擎进行转换
       return SpringUtil.getBean(ConvertEngine2PdfServiceImpl.class).convert(excelFile, htmlFilePath+".pdf");
    }

    private static boolean checkXlsxStream(File excelFile) {
        try {
            Workbook workbook = StreamingReader.builder()
                    .rowCacheSize(1024)//读取到内存中的行数，默认10
                    .bufferSize(40960)//读取资源，缓存到内存的字节大小。默认1024
                    .open(excelFile);//打开资源。只能是xlsx文件
            if (workbook != null) {
                // 遍历sheets
                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);
                    if (sheet.getLastRowNum() > 5000) {
                        log.warn("最大行：{}", sheet.getLastRowNum());
                        return false;
                    }
                    for (Row row : sheet) {
                        if (row.getLastCellNum() > 100) {
                            log.warn("最大列：{}", row.getLastCellNum());
                            return false;
                        }
                    }
                }
            }
        } catch (OLE2NotOfficeXmlFileException e) {
            // 兼容老版本xlsx
            return true;
        }
        return true;
    }

    static class ToHtml {
        private final Workbook workbook;
        private final DataFormat dataFormat;
        private final Appendable output;
        private boolean completeHTML;
        private Formatter out;
        private boolean gotBounds;
        private int firstColumn;
        private int endColumn;
        private HtmlHelper helper;
        private String formatString;
        // 图片 row col 信息， key: col_row, value: img url
        private Map<String, List<String>> sheetPictures;

        private static final String DEFAULTS_CLASS = "excelDefaults";
        private static final String COL_HEAD_CLASS = "colHeader";
        private static final String ROW_HEAD_CLASS = "rowHeader";
        private static final String SHEET_CLASS = "sheetBody";

        private static final Map<Short, String> ALIGN = mapFor(new Short("1"), "left",
                new Short("2"), "center", new Short("3"), "right", new Short("4"), "left",
                new Short("5"), "left", new Short("6"), "center");

        private static final Map<Short, String> VERTICAL_ALIGN = mapFor(
                new Short("2"), "bottom", new Short("1"), "middle", new Short("0"),
                "top");

        private static final Map<Short, String> BORDER = mapFor(BORDER_DASH_DOT,
                "dashed 1pt", BORDER_DASH_DOT_DOT, "dashed 1pt", BORDER_DASHED,
                "dashed 1pt", BORDER_DOTTED, "dotted 1pt", BORDER_DOUBLE,
                "double 3pt", BORDER_HAIR, "solid 1px", BORDER_MEDIUM, "solid 2pt",
                BORDER_MEDIUM_DASH_DOT, "dashed 2pt", BORDER_MEDIUM_DASH_DOT_DOT,
                "dashed 2pt", BORDER_MEDIUM_DASHED, "dashed 2pt", BORDER_NONE,
                "none", BORDER_SLANTED_DASH_DOT, "dashed 2pt", BORDER_THICK,
                "solid 3pt", BORDER_THIN, "solid 1pt");

        private static final String EXCEL_STYLE_CSS = " body{ margin: 0; padding: 0; } img{ max-width: 100%; } .sheetBody{ padding-bottom: 40px; } #sheetmenu{ list-style-type: none; position: fixed; bottom: 0; margin: 0; padding: 0; font-family: Î¢ÈíÑÅºÚ; font-size: 12px; background: #fff; width: 100%; border: 1px solid silver; } #sheetmenu li{ float: left; padding: 5px 15px; cursor: pointer; color: #334466; background-color: #fff; border: 1px solid silver; margin-left: -1px; margin-top: -1px; }   .excelDefaults { background-color: white; color: black; text-decoration: none; direction: ltr; text-transform: none; text-indent: 0; letter-spacing: 0; word-spacing: 0; white-space: normal; unicode-bidi: normal; vertical-align: 0; background-image: none; text-shadow: none; list-style-image: none; list-style-type: none; padding: 0; margin: 0; border-collapse: collapse; white-space: pre; vertical-align: bottom; font-style: normal; font-variant: normal; font-weight: normal; font-size: 10pt; text-align: right; } .excelDefaults td { padding: 1px 5px; border: 1px solid silver; } .excelDefaults .colHeader { background-color: #E4ECF7; border: 1px solid #9EB6CE; text-align: center; padding: 1px 5px; color: #27413E; font-weight: normal; } .excelDefaults .rowHeader { background-color: #E4ECF7; border: 1px solid #9EB6CE; text-align: center; padding: 1px 5px; color: #27413E; }";

        @SuppressWarnings({"unchecked"})
        private static <K, V> Map<K, V> mapFor(Object... mapping) {
            Map<K, V> map = new HashMap<K, V>();
            for (int i = 0; i < mapping.length; i += 2) {
                map.put((K) mapping[i], (V) mapping[i + 1]);
            }
            return map;
        }

        /**
         * 创建转换器对象
         *
         * @param wb     工作簿
         * @param output html输出流
         */
        public static ToHtml create(Workbook wb, Appendable output) {
            return new ToHtml(wb, output);
        }


        /**
         * Creates a new converter to HTML for the given workbook. This attempts to
         * detect whether the input is XML (so it should create an
         * {@link XSSFWorkbook} or not (so it should create an {@link HSSFWorkbook}
         * ).
         *
         * @param in     The input stream that has the workbook.
         * @param output Where the HTML output will be written.
         * @return An object for converting the workbook to HTML.
         */
        public static ToHtml create(InputStream in, Appendable output)
                throws IOException {
            Workbook workbook = WorkbookFactory.create(in);
            return create(workbook, output);
        }

        private ToHtml(Workbook workbook, Appendable output) {
            if (workbook == null)
                throw new NullPointerException("workbook");
            if (output == null)
                throw new NullPointerException("output");
            this.workbook = workbook;
            this.dataFormat = workbook.createDataFormat();  //保存dataFormat
            this.output = output;
            setupColorMap();

        }

        private void setupColorMap() {
            if (workbook instanceof HSSFWorkbook)
                helper = new HSSFHtmlHelper((HSSFWorkbook) workbook);
            else if (workbook instanceof XSSFWorkbook)
                helper = new XSSFHtmlHelper((XSSFWorkbook) workbook);
            else
                throw new IllegalArgumentException("unknown workbook type: "
                        + workbook.getClass().getSimpleName());
        }

        public static File excelToHtml(InputStream in, String htmlPath, String imgPath) {
            try {
                File htmlFile = FileUtil.file(htmlPath);
                FileUtil.mkdir(imgPath);

                @Cleanup FileOutputStream os = new FileOutputStream(htmlFile);
                ToHtml toHtml = create(in, new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)));
                toHtml.setCompleteHTML(true);
                toHtml.printPage(imgPath);
                return htmlFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void setCompleteHTML(boolean completeHTML) {
            this.completeHTML = completeHTML;
        }

        public void printPage(String imgPath) throws IOException {
            try {
                ensureOut();
                //	if (completeHTML) {
                out.format("<!DOCTYPE html>%n");
                out.format("<html>%n");
                out.format("<head>%n");
                out.format("<META http-equiv='Content-Type' content='text/html; charset=utf-8' />%n");
                out.format("</head>%n");
                out.format("<body>%n");
                //	}

                print(imgPath);

                //	if (completeHTML) {
                out.format("</body>%n");
                out.format("</html>%n");
                //	}
            } finally {
                if (out != null)
                    out.close();
                if (output instanceof Closeable) {
                    Closeable closeable = (Closeable) output;
                    closeable.close();
                }
            }
        }

        public void print(String imgPath) {
            printInlineStyle();
            printSheets(imgPath);
            out.format("<br/>%n");
            out.format("<br/>%n");
            out.format("<br/>%n");
            out.format("<br/>%n");
            out.format("<br/>%n");
        }

        private void printInlineStyle() {
            // out.format("<link href=\"excelStyle.css\" rel=\"stylesheet\" type=\"text/css\">%n");
            out.format("<style type=\"text/css\">%n");
            printStyles();
            out.format("</style>%n");
        }

        private void ensureOut() {
            if (out == null)
                out = new Formatter(output);
        }

        public void printStyles() {
            ensureOut();

            // First, copy the base css
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(EXCEL_STYLE_CSS.getBytes())))) {
                //			System.out.println("excelStyle.css came from: " + getClass().getResource("excelStyle.css").getPath());
                String line;
                while ((line = in.readLine()) != null) {
                    out.format("%s%n", line);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Reading standard css", e);
            }

            // now add css for each used style
            Set<Short> seen = new HashSet<Short>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                Iterator<Row> rows = sheet.rowIterator();
                while (rows.hasNext()) {
                    Row row = rows.next();
                    for (Cell cell : row) {
                        CellStyle style = cell.getCellStyle();
                        Short styleId = style.getIndex();
                        if (!seen.contains(styleId)) {
                            printStyle(style);
                            seen
                                    .add(styleId);
                        }
                    }
                }
            }
        }

        private void printStyle(CellStyle style) {
            out.format(".%s .%s {%n", DEFAULTS_CLASS, styleName(style));
            styleContents(style);
            out.format("}%n");
        }

        private void styleContents(CellStyle style) {
            styleOut("text-align", style.getAlignment().getCode(), ALIGN);
            styleOut("vertical-align", style.getVerticalAlignment().getCode(), VERTICAL_ALIGN);
            fontStyle(style);
            borderStyles(style);
            helper.colorStyles(style, out);
        }

        private void borderStyles(CellStyle style) {
            styleOut("border-left", style.getBorderLeft().getCode(), BORDER);
            styleOut("border-right", style.getBorderRight().getCode(), BORDER);
            styleOut("border-top", style.getBorderTop().getCode(), BORDER);
            styleOut("border-bottom", style.getBorderBottom().getCode(), BORDER);
        }

        private void fontStyle(CellStyle style) {
            Font font = workbook.getFontAt(style.getFontIndex());

            if (font.getBold())
                out.format("  font-weight: bold;%n");
            if (font.getItalic())
                out.format("  font-style: italic;%n");

            int fontheight = font.getFontHeightInPoints();
            if (fontheight == 9) {
                // fix for stupid ol Windows
                fontheight = 10;
            }
            out.format("  font-size: %dpt;%n", fontheight);

            // Font color is handled with the other colors
        }

        private String styleName(CellStyle style) {
            if (style == null)
                style = workbook.getCellStyleAt((short) 0);
            StringBuilder sb = new StringBuilder();
            Formatter fmt = new Formatter(sb);
            fmt.format("style_%02x", style.getIndex());
            String styleName = fmt.toString();
            fmt.close();
            return styleName;
        }

        private <K> void styleOut(String attr, K key, Map<K, String> mapping) {
            String value = mapping.get(key);
            if (value != null) {
                out.format("  %s: %s;%n", attr, value);
            }
        }

        private static CellType ultimateCellType(Cell c) {
            CellType type = c.getCellType();
            if (type == CellType.FORMULA)
                type = c.getCachedFormulaResultType();
            return type;
        }

        // 打印所有的sheets
        private void printSheets(String imgPath) {
            ensureOut();

            int sheetNumbers = workbook.getNumberOfSheets();
            Sheet sheet;
            Map<String, String> linkmap = new LinkedHashMap<String, String>();
            String uuid;
            boolean flag = true;
            // 遍历所有sheet
            for (int i = 0; i < sheetNumbers; i++) {
                uuid = UUID.randomUUID().toString();
                sheet = workbook.getSheetAt(i);
                if (workbook.isSheetHidden(i)) {
                    continue;
                }
                // 存储
                linkmap.put(uuid, workbook.getSheetName(i));
                if (flag) {
                    out.format("<div id='" + uuid + "' class='" + SHEET_CLASS + "'>%n");
                    flag = false;
                } else
                    out.format("<div id='" + uuid + "' class='" + SHEET_CLASS + "' style='display:none;'>%n");
                // 生成图片信息
                getImg(sheet, imgPath);
                printSheet(sheet);
                out.format("</div>%n");
            }

            //添加sheet菜单
            addSheetMenu(linkmap);
            //添加sheet菜单点击事件
            addSheetMenuAction(linkmap);

        }

        /**
         * 添加底部菜单选项
         *
         * @param linkmap
         */
        public void addSheetMenu(Map<String, String> linkmap) {
            out.format("<ul id='sheetmenu' >%n");
            int flag = 0;
            for (Map.Entry<String, String> entry : linkmap.entrySet()) {
                if (flag == 0)
                    out.format("<li name='tosheet' style='color:#008ff3;background-color:lightgray;' linkto='" + entry.getKey() + "'>%n");
                else
                    out.format("<li name='tosheet' linkto='" + entry.getKey() + "'>%n");
                out.format("%s", entry.getValue());
                out.format("</li>%n");
                flag++;
            }
            out.format("</ul>%n");
        }

        /**
         * 添加菜单操作
         *
         * @param linkmap
         */
        public void addSheetMenuAction(Map<String, String> linkmap) {
            out.format("<script type='text/javascript'>%n");
            out.format("var elements = document.getElementById('sheetmenu').children;%n");
            out.format("for(var i=0,len=elements.length;i<len;i++){ %n");
            out.format("elements[i].onclick=function(){ %n");
            out.format("var id = this.getAttribute('linkto'); %n");
            out.format("for(var j=0;j<len;j++){ %n");
            out.format("elements[j].style.color = '#000000'; %n");
            out.format("elements[j].style.background = '#fff'; %n");
            out.format("document.getElementById(elements[j].getAttribute('linkto')).style.display = 'none'; %n");
            out.format(" } %n");
            out.format("document.getElementById(id).style.display = 'block'; %n");
            out.format("this.style.color = '#008ff3'; %n");
            out.format("this.style.background = 'lightgray'; %n");
            out.format("} %n");
            out.format("} %n");
            out.format("</script>%n");
        }


        public void printSheet(Sheet sheet) {
            ensureOut();
            out.format("<table class=%s>%n", DEFAULTS_CLASS);
            printCols(sheet);
            printSheetContent(sheet);
            out.format("</table>%n");
        }

        private void printCols(Sheet sheet) {
            out.format("<col/>%n");
            ensureColumnBounds(sheet);
            for (int i = firstColumn; i < endColumn; i++) {
                out.format("<col/>%n");
            }
        }

        private void ensureColumnBounds(Sheet sheet) {
            //if (gotBounds)
            //	return;

            Iterator<Row> iter = sheet.rowIterator();
            firstColumn = (iter.hasNext() ? Integer.MAX_VALUE : 0);
            endColumn = 0;
            while (iter.hasNext()) {
                Row row = iter.next();
                short firstCell = row.getFirstCellNum();
                if (firstCell >= 0) {
                    firstColumn = Math.min(firstColumn, firstCell);
                    endColumn = Math.max(endColumn, row.getLastCellNum());
                    if (endColumn > 200) {
                        endColumn = 200;
                    }

                }
            }
            gotBounds = true;
        }

        private void printColumnHeads(Sheet sheet) {
            out.format("<thead>%n");
            out.format("  <tr class=%s>%n", COL_HEAD_CLASS);
            out.format("    <th class=%s>&#x25CA;</th>%n", COL_HEAD_CLASS);
            // noinspection UnusedDeclaration
            StringBuilder colName = new StringBuilder();
            for (int i = firstColumn; i < endColumn; i++) {
                colName.setLength(0);
                int cnum = i;
                do {
                    colName.insert(0, (char) ('A' + cnum % 26));
                    cnum /= 26;
                } while (cnum > 0);
                // System.out.println("colwidth===>"+sheet.getColumnWidth(i)/36);
                out.format("    <th class=%s style='width:%spx;'>%s</th>%n",
                        COL_HEAD_CLASS, sheet.getColumnWidth(i) / 38, colName);
            }
            out.format("  </tr>%n");
            out.format("</thead>%n");
        }

        private void printSheetContent(Sheet sheet) {
            try {
                printColumnHeads(sheet);

                out.format("<tbody>%n");
                int lastrow = sheet.getLastRowNum();
                Row rowTemp;
                //		if(lastrow>3000){
                //			lastrow=3000;
                //		}
                for (int m = 0; m <= lastrow; m++) {
                    rowTemp = sheet.getRow(m);
                    //空行填充
                    if (rowTemp == null) {
                        printEmptyRow(sheet, m + 1);
                    } else {
                        // 行高处理
                        out.format("  <tr style='height:%spx;'>%n", rowTemp.getHeightInPoints());
                        out.format("    <td class=%s>%d</td>%n", ROW_HEAD_CLASS, rowTemp.getRowNum() + 1);
                        for (int i = firstColumn; i < endColumn; i++) {
                            try {
                                String content = "&nbsp;";
                                String attrs = "";
                                CellStyle style = null;
                                if (i >= rowTemp.getFirstCellNum() && i < rowTemp.getLastCellNum()) {
                                    Cell cell = rowTemp.getCell(i);
                                    if (cell != null) {
                                        style = cell.getCellStyle();
                                        attrs = tagStyle(cell, style);
                                        // Set the value that is rendered for the cell
                                        // also applies the format
                                        //							CellFormat cf = CellFormat.getInstance(style.getDataFormatString());

                                        //不需要每次都获取style再转换 dataFormat String
                                        formatString = dataFormat.getFormat(style.getDataFormat());
                                        if (formatString != null) { //兼容拿不到 formatString 报异常的情况，否则可能出现值打印出部分行的情况
                                            //如果是数字类型,获取一下是否是日期
                                            if (cell.getCellType() == CellType.NUMERIC) {
                                                String dateStyle = getDateStyle(cell);
                                                if (dateStyle.isEmpty()) {//不是日期，按之前逻辑处理
                                                    CellFormat cf = CellFormat.getInstance(dataFormat.getFormat(style.getDataFormat()));
                                                    CellFormatResult result = cf.apply(cell);
                                                    content = result.text;
                                                } else {//是日期，按日期处理
                                                    SimpleDateFormat sdf = new SimpleDateFormat(dateStyle);
                                                    double value = cell.getNumericCellValue();
                                                    Date date = DateUtil.getJavaDate(value);
                                                    content = sdf.format(date);
                                                }
                                            } else {
                                                CellFormat cf = CellFormat.getInstance(dataFormat.getFormat(style.getDataFormat()));
                                                CellFormatResult result = cf.apply(cell);
                                                content = result.text;
                                            }
                                        }
                                        //内容转义处理
                                        content = HtmlUtils.htmlEscape(content);
                                        if (content.equals("")) {
                                            content = "&nbsp;";
                                        }
                                        if (sheetPictures.containsKey(i + "_" + m)) {
                                            content += String.join("", sheetPictures.get(i + "_" + m));
                                        }
                                    }
                                }
                                out.format("    <td class=%s %s>%s</td>%n", styleName(style),
                                        attrs, content);
                            } catch (Exception | Error ex) {
                                continue;
                            }
                        }
                        out.format("  </tr>%n");
                    }
                }

                //打印空行，避免最后一行被遮盖
                //	    printEmptyRow(sheet, lastrow + 2);
                //	    printEmptyRow(sheet, lastrow + 3);

            } catch (Exception | Error e) {
                //e.printStackTrace();
            }

            out.format("</tbody>%n");
        }

        private static String getDateStyle(Cell cell) {

            String result;
            switch (cell.getCellStyle().getDataFormat()) {
                case 14:
                    result = "yyyy-MM-dd";
                    break;
                case 31:
                case 176:
                case 180:
                case 181:
                    result = "yyyy年MM月dd日";
                    break;
                case 57:
                case 178:
                case 182:
                    result = "yyyy年MM月";
                    break;
                case 58:
                case 177:
                case 183:
                    result = "MM月dd日";
                    break;
                case 179:
                case 184:
                    result = "EEEE";
                    break;
                case 185:
                case 188:
                    result = "yyyy-MM-dd";
                    break;
                case 186:
                    result = "yyyy-MM-dd hh:mm:ss a";
                    break;
                case 22:
                case 187:
                    result = "yyyy-MM-dd HH:mm";
                    break;
                case 189:
                    result = "MM-dd";
                    break;
                case 190:
                case 191:
                    result = "MM-dd-yyyy";
                    break;
                case 192:
                case 195:
                    result = "HH:mm:ss";
                    break;
                case 193:
                    result = "HH:mm";
                    break;
                case 194:
                    result = "hh:mm a";

                    break;
                case 196:
                    result = "hh:mm:ss a";
                    break;
                case 197:
                case 201:
                    result = "HH时mm分";
                    break;
                case 198:
                    result = "HH时mm分ss秒";
                    break;
                case 199:
                case 202:
                    result = "ahh时mm分";
                    break;
                case 200:
                    result = "ahh时mm分ss秒";
                    break;
                case 0:
                    result = "";
                    break;
                default:
                    result = "";
                    break;
            }
            return result;
        }

        private void printEmptyRow(Sheet sheet, int rowNum) {
            out.format("  <tr style='height:%spx;'>%n", sheet.getDefaultRowHeightInPoints());
            out.format("    <td class=%s>%d</td>%n", ROW_HEAD_CLASS, rowNum);
            for (int i = firstColumn; i < endColumn; i++) {
                out.format("    <td ></td>%n");
            }
            out.format("  </tr>%n");
        }

        private String tagStyle(Cell cell, CellStyle style) {
            if (style.getAlignment() == HorizontalAlignment.GENERAL) {
                switch (ultimateCellType(cell)) {
                    case STRING:
                        return "style=\"text-align: left;\"";
                    case BOOLEAN:
                    case ERROR:
                        return "style=\"text-align: center;\"";
                    case NUMERIC:
                    default:
                        // "right" is the default
                        break;
                }
            }
            return "";
        }

        //图片处理
        public void getImg(Sheet sheetNow, String imgPath) {
            sheetPictures = new HashMap<>();
            try {
                // xls图片处理
                if (workbook instanceof HSSFWorkbook) {
                    HSSFSheet sheet = (HSSFSheet) sheetNow;
                    List<HSSFPictureData> allPictures = ((HSSFWorkbook) workbook).getAllPictures();
                    if (allPictures.size() != 0) {
                        for (HSSFShape shape : sheet.getDrawingPatriarch().getChildren()) {
                            HSSFClientAnchor anchor = (HSSFClientAnchor) shape.getAnchor();
                            if (shape instanceof HSSFPicture) {
                                HSSFPicture pic = (HSSFPicture) shape;
                                String picIndex = anchor.getRow1() + "_" + anchor.getCol1();

                                HSSFClientAnchor anchor1 = pic.getClientAnchor();
                                int dx1 = anchor1.getDx1();
                                int dx2 = anchor1.getDx2();

                                String imgUrl = saveAndGetImgUrl(imgPath, pic.getShapeName(), pic.getPictureData().getData()
                                        , pic.getPictureData().suggestFileExtension(), dx2 - dx1);
                                sheetPictures.compute(picIndex, (k, v) -> {
                                    if (v == null) {
                                        v = new ArrayList<>();
                                    }
                                    v.add(imgUrl);
                                    return v;
                                });
                            }
                        }
                    }
                    // xlsx处理
                } else if (workbook instanceof XSSFWorkbook) {
                    XSSFSheet sheet = (XSSFSheet) sheetNow;
                    for (POIXMLDocumentPart dr : sheet.getRelations()) {
                        if (dr instanceof XSSFDrawing) {
                            XSSFDrawing drawing = (XSSFDrawing) dr;

                            List<XSSFShape> shapes = drawing.getShapes();
                            for (XSSFShape shape : shapes) {
                                if (shape instanceof XSSFPicture) {
                                    XSSFPicture pic = (XSSFPicture) shape;
                                    XSSFClientAnchor anchor = pic.getPreferredSize();
                                    CTMarker ctMarker = anchor.getFrom();

                                    int dx1 = anchor.getDx1();
                                    int dx2 = anchor.getDx2();

                                    // 这里dx2 - dx1 和 dy2 - dy1给出了图片在工作表上的宽度和高度
                                    try {
                                        String imgUrl = saveAndGetImgUrl(imgPath, pic.getShapeName(), pic.getPictureData().getData()
                                                , pic.getPictureData().suggestFileExtension(), dx2 - dx1);
                                        sheetPictures.compute(ctMarker.getCol() + "_" + ctMarker.getRow(), (k, v) -> {
                                            if (v == null) {
                                                v = new ArrayList<>();
                                            }
                                            v.add(imgUrl);
                                            return v;
                                        });
                                    } catch (Exception | Error e) {
                                        System.err.println("图片处理异常");
                                        e.printStackTrace();
                                    }
                                } else {
                                    XSSFObjectData shape1 = (XSSFObjectData) shape;
                                    String fileName = shape1.getFileName();
                                    FileUtil.writeBytes(shape1.getObjectData(), "E:\\ltian_project\\java\\convert-preview\\outtemp\\" + fileName);
                                }
                            }
                        }

                    }
                }
            } catch (Exception | Error e) {
                //e.printStackTrace();
            }
        }

        @SneakyThrows
        private static String saveAndGetImgUrl(String imgPath, String shapeName, byte[] data, String ext, int cellWith) {
            cellWith = Math.abs(cellWith) / Units.EMU_PER_PIXEL;

            FileUtil.mkdir(imgPath);
            String imgFilePath = imgPath + "/" + shapeName + "." + ext;
            File picFile = new File(imgFilePath);

            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(data));
            int imgWidth = bi.getWidth();
            if (imgWidth > 300) {
                Thumbnails.of(bi).
                        scale((double) cellWith / imgWidth). // 图片缩放80%, 不能和size()一起使用
                        outputQuality(ConvertDocConfigBase.picQuality). // 图片质量压缩80%
                        toFile(imgFilePath);
            } else {
                FileUtil.writeBytes(data, picFile);
            }
            return "<img src='/api/download?urlPath=" + AesUtil.encryptStr(imgFilePath) + "'/>";
        }

        // 获取x坐标
        public static int getXposition(Sheet sheet, int column) {
            int x = 0;
            for (int i = 0; i < column; i++) {
                x += sheet.getColumnWidth(i) / 36;
            }
            return x + 40;
        }

        // 获取y坐标
        public static float getYposition(Sheet sheet, int row) {
            float y = 0.0f;
            for (int i = 0; i < row; i++) {
                if (sheet.getRow(i) != null) {
                    y += (sheet.getRow(i).getHeightInPoints() + 6);
                } else {
                    y += (sheet.getDefaultRowHeightInPoints() + 6);
                }
            }
            return y + 24;
        }

    }
}
