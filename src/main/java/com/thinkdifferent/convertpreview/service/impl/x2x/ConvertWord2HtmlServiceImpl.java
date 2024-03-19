package com.thinkdifferent.convertpreview.service.impl.x2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.service.PoiConvertTypeService;
import com.thinkdifferent.convertpreview.utils.AesUtil;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * doc转html，默认引擎，配置其他引擎不会初始化
 *
 * @author ltian
 * @version 1.0
 * @date 2024/1/11 14:34
 */
@Service
@ConditionalOnProperty(name = "convert.preview.poi.word", havingValue = "true")
public class ConvertWord2HtmlServiceImpl implements ConvertTypeService, PoiConvertTypeService {
    /**
     * 根据类型进行转换
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        String htmlFilePath = targetDir + ".html";
        String strExt = FileUtil.extName(inputFile);
        String strFileName = FileUtil.mainName(htmlFilePath);
        if("docx".equalsIgnoreCase(strExt)){
            return docxToHtml(inputFile, htmlFilePath, htmlFilePath + "/" + strFileName);
        }else if("doc".equalsIgnoreCase(strExt)){
            return docToHtml(inputFile, htmlFilePath, htmlFilePath + "/" + strFileName);
        }

        return null;
    }

    /**
     * docx转html
     *
     * @param inputFile      输入文件
     * @param outputPath     转换后文件全路径，带后缀
     * @param outputPathFile 转换后html文件全路径，带后缀
     * @return 转换后文件
     */
    private File docxToHtml(File inputFile, String outputPath, String outputPathFile)
            throws IOException {
        @Cleanup InputStream in = new FileInputStream(inputFile);
        XWPFDocument document = new XWPFDocument(in);

        XHTMLOptions options = XHTMLOptions.create()
                .URIResolver(s -> {
                    File picFile = FileUtil.file(outputPath, s);
                    // 下载地址
                    return "/api/download?urlPath=" + AesUtil.encryptStr(FileUtil.getCanonicalPath(picFile));
                });

        options.setExtractor((s, bytes) -> {
            // s: name; bytes: 图片 byte 保存图片
            File picFile = FileUtil.file(outputPath, s);
            try {
                FileUtil.writeBytes(bytes, picFile);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        XHTMLConverter.getInstance().convert(document, out, options);

        out.close();
        String content = new String(out.toByteArray());
        String compile = "<style([^<]+)</style>";
        Pattern pattern = Pattern.compile(compile);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            content = "<!DOCTYPE html>" + matcher.replaceFirst(new StringBuilder(String.valueOf(matcher.group())).append("<style  type='text/css' > table{border-collapse: collapse;} table td{border:1px solid #000;} img {max-width:100%;}</style><META http-equiv='Content-Type' content='text/html; charset=GBK' />").toString());
        }
        FileUtil.writeBytes(content.getBytes(), outputPathFile);
        return new File(outputPathFile);
    }

    /**
     * doc转html
     *
     * @param inputFile      输入文件
     * @param outputPath     转换后文件全路径，带后缀
     * @param outputPathFile 转换后html文件全路径，带后缀
     * @return 转换后文件
     */
    @SneakyThrows
    private File docToHtml(File inputFile, String outputPath, String outputPathFile) {
        @Cleanup InputStream in = new FileInputStream(inputFile);
        HWPFDocument wordDocument = new HWPFDocument(in);

        WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .newDocument());
        wordToHtmlConverter.setPicturesManager((content, pictureType, suggestedName, widthInches, heightInches) -> {
            // 保存图片
            File picFile = FileUtil.file(outputPath, suggestedName);
            FileUtil.writeBytes(content, picFile);
            // 返回图片的下载路径
            return "/api/download?urlPath=" + AesUtil.encryptStr(FileUtil.getCanonicalPath(picFile));
        });
        wordToHtmlConverter.processDocument(wordDocument);

        Document htmlDocument = wordToHtmlConverter.getDocument();

        @Cleanup ByteArrayOutputStream out = new ByteArrayOutputStream();
        DOMSource domSource = new DOMSource(htmlDocument);
        StreamResult streamResult = new StreamResult(out);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty("encoding", "utf-8");
        serializer.setOutputProperty("indent", "yes");
        serializer.setOutputProperty("method", "html");
        serializer.transform(domSource, streamResult);
        String content = new String(out.toByteArray());

        // 使用Jsoup解析并输出格式化、闭合正确的HTML
        org.jsoup.nodes.Document doc = Jsoup.parse(content);
        // 选择所有的<meta>元素并删除它们
        Elements elements = doc.getElementsByTag("meta");
        for (int i = 1; i < elements.size(); i++) {
            elements.get(i).remove();
        }

        content = delHTMLTag(doc.html()
                .replace("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">",
                        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>"));

        String compile = "<style([^<]+)</style>";
        Pattern pattern = Pattern.compile(compile);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            content = "<!DOCTYPE html>" + matcher.replaceFirst(new StringBuilder(String.valueOf(matcher.group())).append("<style  type='text/css' > img {max-width:100%;}</style>").toString());
        }

        FileUtil.writeBytes(content.getBytes(), outputPathFile);
        return new File(outputPathFile);
    }

    private static String delHTMLTag(String htmlStr) {
        String regEx_space = "\t|\r|\n";
        Pattern p_html1 = Pattern.compile(regEx_space, 2);
        Matcher m_html1 = p_html1.matcher(htmlStr);
        htmlStr = m_html1.replaceAll("");
        String regEx_html = "<p[^>]*><span[^>]*>([0-9]*(&#12289;&#8203;&nbsp;))</span></p>";
        Pattern p_html = Pattern.compile(regEx_html, 2);
        Matcher m_html = p_html.matcher(htmlStr);
        htmlStr = m_html.replaceAll("");
        return htmlStr;
    }

}
