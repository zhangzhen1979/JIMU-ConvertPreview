package com.thinkdifferent.convertpreview.utils.htmlUtil;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.List;
import java.util.Map;

@Data
@Log4j2
public class TableSizeCalculate {
    /**
     * 表格Table宽度（px）
     */
    private int widthPx;
    /**
     * 表格Table高度（px）
     */
    private int heightPx;
    /**
     * 表格行宽度（px）
     */
    private int tdWidthPx;
    /**
     * 表格行高度（px）
     */
    private int tdHeightPx;


    public TableSizeCalculate set(Map<String, String> mapMarkData) {
        if (mapMarkData.containsKey("tableWidth") && !mapMarkData.get("tableWidth").isEmpty()) {
            this.setWidthPx(Integer.valueOf(mapMarkData.get("tableWidth")));
        }
        if (mapMarkData.containsKey("tableHeight") && !mapMarkData.get("tableHeight").isEmpty()) {
            this.setHeightPx(Integer.valueOf(mapMarkData.get("tableHeight")));
        }

        if (mapMarkData.containsKey("tdHeight") && !mapMarkData.get("tdHeight").isEmpty()) {
            this.setTdWidthPx(Integer.valueOf(mapMarkData.get("tdHeight")));
        }
        if (mapMarkData.containsKey("tdWidth") && !mapMarkData.get("tdWidth").isEmpty()) {
            this.setTdWidthPx(Integer.valueOf(mapMarkData.get("tdWidth")));
        }


        return this;
    }

    public TableSizeCalculate get(String strHtmlPath, String strHtml) {
        try {
            Document document = null;

            if (strHtml != null && !strHtml.isEmpty()) {
                document = DocumentHelper.parseText(strHtml);
            } else {
                File fileHtml = new File(strHtmlPath);
                if (fileHtml != null
                        && fileHtml.exists()
                        && fileHtml.length() > 0) {
                    SAXReader reader = new SAXReader();
                    document = reader.read(fileHtml);
                } else {
                    log.error("HTML文件不存在：", strHtmlPath);
                }
            }

            Element elmHtml = document.getRootElement();
            Element elmBody = elmHtml.element("body");
            Element elmTable = elmBody.element("table");

            List<Element> listTR = elmTable.elements();
            if (listTR != null && listTR.size() > 0) {
                // 获取表格行数，计算单个单元格高度
                tdHeightPx = (heightPx) / listTR.size();

                // 获取表格列数，计算单个单元格宽度
                Element elmTR = listTR.get(0);
                List<Element> listTD = elmTR.elements();

                if (listTD != null && listTD.size() > 0) {
                    tdWidthPx = widthPx / listTD.size();
                }
            }
        } catch (Exception | Error e) {
            log.error("异常", e);
        }

        return this;
    }


}
