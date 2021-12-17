package org.ofd.render.dir;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 对象序列化
 *
 * @author 权观宇
 * @since 2020-01-20 14:45:02
 */
public class DocObjDump {

    /**
     * 序列化文档对象
     *
     * @param e  文档对象
     * @param to 目标目录
     * @throws IOException IO异常
     */
    public static void dump(Element e, Path to) throws IOException {
        if (e == null) {
            return;
        }
        if (to == null) {
            throw new IllegalArgumentException("文档元素序列化目标目录（to）为空");
        }
        if (Files.notExists(to)) {
            if (Files.notExists(to.getParent())) {
                Files.createDirectories(to.getParent());
            }
            Files.createFile(to);
        }

        Document doc = DocumentHelper.createDocument();
        //System.out.println(e.asXML());
        //System.out.println(doc.asXML());
        doc.add(e);
        try (OutputStream out = Files.newOutputStream(to)) {
            XMLWriter writeToFile = new XMLWriter(out);
            writeToFile.write(doc);
            writeToFile.close();
        }
    }

    /**
     * 序列化文档对象
     *
     * @Author: Lianglx
     * @Description: 把文档对象的二进制数据dump到map中，不写临时文件
     *
     * @param e
     * @param loc
     * @param virtualFileMap
     * @throws IOException
     */
    public static void dump(Element e, String loc, Map<String, byte[]> virtualFileMap) throws IOException {
        if (e == null) {
            return;
        }

        Document doc = DocumentHelper.createDocument();
        //System.out.println(e.asXML());
        //System.out.println(doc.asXML());
        doc.add(e);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XMLWriter writeToFile = new XMLWriter(bos);
            writeToFile.write(doc);
            writeToFile.close();
            virtualFileMap.put(loc, bos.toByteArray());
        }
    }
}
