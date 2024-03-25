package com.thinkdifferent.convertpreview.service.impl.html2x;

import com.lowagie.text.pdf.BaseFont;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.extern.log4j.Log4j2;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 18:02
 */
@Service
@Log4j2
public class ConvertHtml2PdfServiceImpl implements ConvertTypeService {
    /**
     * html转pdf
     *
     * @param objInputFile      传入的文件，格式已确定
     * @param strOutputFilePath 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @Override
    public File convert0(File objInputFile, String strOutputFilePath) {
        String strInputFile = SystemUtil.beautifulFilePath(objInputFile.toString());
        // 开始时间
        long stime = System.currentTimeMillis();

        boolean blnFlag = html2pdf(strInputFile, strOutputFilePath + ".pdf");

        // 结束时间
        long etime = System.currentTimeMillis();
        // 计算执行时间
        if (blnFlag) {
            log.info("Html Convert PDF success, Use time is: " + (etime - stime) + " ms...");
            return new File(strOutputFilePath + ".pdf");
        } else {
            log.info("Html Convert PDF fail, Use time is: " + (etime - stime) + " ms...");
            return null;
        }
    }

    private boolean html2pdf(String strHtmlFile, String strPdfFile) {
        try {
            //创建PDf文件
            ITextRenderer renderer = new ITextRenderer();
            ITextFontResolver fontResolver = renderer.getFontResolver();

            File fileFontPath;
            if (cn.hutool.system.SystemUtil.getOsInfo().isWindows()) {
                // 读取Windows字体文件夹
                fileFontPath = new File(SystemUtil.beautifulPath(System.getenv("WINDIR")) + "Fonts/");
                scanFonts(fontResolver, fileFontPath);

                // 读取User文件夹下的字体文件夹
                fileFontPath = new File(SystemUtil.beautifulPath(System.getenv("USERPROFILE")) +
                        "AppData/Local/Microsoft/Windows/Fonts/");
                scanFonts(fontResolver, fileFontPath);

            } else if (cn.hutool.system.SystemUtil.getOsInfo().isLinux()) {
                fileFontPath = new File("/usr/share/fonts/");
                scanFonts(fontResolver, fileFontPath);

            } else if (cn.hutool.system.SystemUtil.getOsInfo().isMacOsX()) {
                fileFontPath = new File("/Library/Fonts/");
                scanFonts(fontResolver, fileFontPath);

                fileFontPath = new File("/System/Library/Fonts/");
                scanFonts(fontResolver, fileFontPath);

                fileFontPath = new File(
                        SystemUtil.beautifulPath(System.getProperty("user.home")) + "/Library/Fonts/");
                scanFonts(fontResolver, fileFontPath);

            } else if (cn.hutool.system.SystemUtil.getOsInfo().isHpUx()) {
                fileFontPath = new File(SystemUtil.beautifulPath(System.getProperty("user.home")) + "/.local/share/fonts/");
                scanFonts(fontResolver, fileFontPath);

                fileFontPath = new File(SystemUtil.beautifulPath(System.getProperty("user.home")) + "/.fonts/");
                scanFonts(fontResolver, fileFontPath);

                fileFontPath = new File("/usr/share/fonts/");
                scanFonts(fontResolver, fileFontPath);

                fileFontPath = new File("/usr/local/share/fonts/");
                scanFonts(fontResolver, fileFontPath);

            } else {
                fileFontPath = new File("/usr/share/fonts/");
                scanFonts(fontResolver, fileFontPath);

            }

            OutputStream osPdf = new FileOutputStream(strPdfFile);

            SAXReader reader = new SAXReader();
            Document document = reader.read(new File(strHtmlFile));
            Element elmRoot = document.getRootElement();

            Element elmBody = elmRoot.element("body");
            String strBodyStyle = elmBody.attributeValue("style");
            if (strBodyStyle == null ||
                    strBodyStyle.isEmpty() ||
                    !strBodyStyle.contains("SimSun")) {
                if (strBodyStyle == null) {
                    strBodyStyle = "";
                } else {
                    strBodyStyle = strBodyStyle + ";";
                }
                strBodyStyle = strBodyStyle + "font-family: SimSun;";
                elmBody.addAttribute("style", strBodyStyle);
            }

            //使用有setDocumentFromString（）方法的jar包
            renderer.setDocumentFromString(document.asXML());
            renderer.layout();
            renderer.createPDF(osPdf);
            osPdf.close();

            return true;

        } catch (Exception | Error e) {
            log.error(e);
            return false;
        }
    }


    private void scanFonts(ITextFontResolver fontResolver, File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        scanFonts(fontResolver, file); // 递归调用遍历子文件夹
                    } else {
                        String strFileName = file.getName();
                        if ("simsun.ttc".equalsIgnoreCase(strFileName) ||
                                "simsun.ttf".equalsIgnoreCase(strFileName) ||
                                "arial.ttf".equalsIgnoreCase(strFileName)) {
                            try {
                                fontResolver.addFont(file.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                            } catch (Exception | Error e) {

                            }
                        }
                    }
                }
            }
        }
    }


}
