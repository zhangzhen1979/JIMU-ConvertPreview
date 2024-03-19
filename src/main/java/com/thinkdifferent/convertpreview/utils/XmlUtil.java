package com.thinkdifferent.convertpreview.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Log4j2
public class XmlUtil {

    /**
     * 将XML字符串保存为文件，存放到指定的文件中。
     *
     * @param strXML        XML字符串
     * @param strTargetFile 文件路径、文件名
     * @throws Exception
     */
    public static void saveXML(String strXML, String strTargetFile)
            throws Exception {
        // 输出xml文件
        Document document = DocumentHelper.parseText(strXML);
        document.setXMLEncoding("UTF-8");

        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8"); // 指定XML编码
        format.setXHTML(true);
        XMLWriter writer = new XMLWriter(
                new FileOutputStream(
                        new File(strTargetFile)
                ),
                format
        );
        writer.write(document);
        writer.close();
    }

    public static String readFile2String(String strXMLFile) throws IOException {
        return String.join("\n", Files.readAllLines(Paths.get(strXMLFile)));
    }


    public static JSONObject xml2json(String xml) throws JsonProcessingException {
        XmlMapper xmlMapper = new XmlMapper();
        JsonNode jsonNode = xmlMapper.readTree(xml);
        JSONObject result = JSONObject.fromObject(jsonNode.toString());

        return result;
    }

    /**
     * 是否是xbrl格式的xml
     *
     * @param fileInput xml文件
     * @return bln
     */
    public static boolean checkFincXml(File fileInput) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(fileInput);
            Element elmRoot = document.getRootElement();

            // 读取文件根节点，判断是否为xbrl。如果是，则走【xml-json-报表】的方式预览。
            if ("xbrl".equalsIgnoreCase(elmRoot.getName())) {
                String strXPath = "/xbrl/link:schemaRef";
                Element elmValue = (Element) elmRoot.selectSingleNode(strXPath);
                if (elmValue != null) {
                    // 如果匹配，则返回true
                    return true;
                }
            } else {
                // 如果不是xbrl，则按照xml报表模板设置检索。
                String strSetPath = SystemUtil.beautifulPath(System.getProperty("user.dir")) + "conf/";
                File fileXmlReportSet = new File(strSetPath + "XmlReport.xml");
                Document documentSet = reader.read(fileXmlReportSet);
                Element elmRootSet = documentSet.getRootElement();
                List<Element> listSetElements = elmRootSet.elements();
                // 循环，判断设置的Element的name是否与XML中取得的href值的前半段相符。
                for (Element elmSetNode : listSetElements) {
                    String strXPath = elmSetNode.attributeValue("path");

                    Element elmNode = (Element) elmRoot.selectSingleNode(strXPath);
                    if (elmNode != null) {
                        // 如果匹配，则返回true
                        return true;
                    }
                }
            }

        } catch (Exception | Error e) {
            log.error(e);
        }

        return false;
    }

}
