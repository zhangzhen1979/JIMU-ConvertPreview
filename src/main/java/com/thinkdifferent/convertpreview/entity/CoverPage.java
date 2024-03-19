package com.thinkdifferent.convertpreview.entity;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.service.impl.html2x.ConvertHtml2PdfServiceImpl;
import com.thinkdifferent.convertpreview.utils.ConvertOfdUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.ofdrw.tool.merge.OFDMerger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * 封面
 *
 * @author 张镇
 * @version 1.0
 * @date 2023-5-31 14:14:47
 */
@Data
public class CoverPage {

    /**
     * 封面html字符串的base64值
     */
    private String base64;

    public static CoverPage get(Map<String, Object> map) {
        CoverPage coverPage = new CoverPage();
        coverPage.setBase64(MapUtil.getStr(map, "base64", null));

        return StringUtils.isNotBlank(coverPage.getBase64()) ?
                coverPage : null;
    }


    /**
     * 根据传入的“封面对象”中的信息，生成封面HTML文件
     *
     * @return 封面HTML文件
     * @throws IOException err
     */
    private File getCoverHtmlFile() {
        // 搞个临时文件名
        String uuid = UUID.randomUUID().toString();
        String path = ConvertDocConfigBase.inPutTempPath;
        String htmlPathFile = path + uuid + ".html";

        if (!StringUtils.isEmpty(this.getBase64())) {
            // 如果是base64，则转换为字符串，存成utf-8编码的html文件
            byte[] bytes = Base64.getDecoder().decode(this.getBase64());
            // 将html存成文件
            FileOutputStream fileOutputStream = null;
            try {
                File file = new File(htmlPathFile);
                //判断文件是否存在
                if (file.exists()) {
                    file.delete();
                }
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(bytes);
                return file;
            } catch (Exception | Error e) {
                e.printStackTrace();
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }

        return null;
    }

    /**
     * 生成PDF版【封面】文件
     *
     * @return
     */
    public File getCoverPDF() {
        if (this.getBase64() != null) {
            // 生成HTML文件
            File fileHTMLCover = getCoverHtmlFile();

            // 生成封面PDF文件
            String strPDFCover = fileHTMLCover.getAbsolutePath() + ".pdf";
            ConvertHtml2PdfServiceImpl html2Pdf = new ConvertHtml2PdfServiceImpl();
            File filePDFCover = html2Pdf.convert(fileHTMLCover, strPDFCover);

            if (filePDFCover.exists()) {
                fileHTMLCover.delete();
            }

            return filePDFCover;
        }
        return null;
    }


    /**
     * PDF文件中加入【封面】
     *
     * @param fileInput
     * @throws IOException
     */
    public File add2Pdf(File fileInput) throws IOException {
        if (this.getBase64() != null) {
            // 生成封面PDF文件
            File filePDFCover = getCoverPDF();

            // 合成到现有PDF文件中，加到第一页。
            // 声明PDF合并工具对象
            PDFMergerUtility pdfMerger = new PDFMergerUtility();
            // 设置合并后的PDF文件的路径和文件名
            String strDestPathFileName = fileInput.getAbsolutePath() + ".pdf";
            pdfMerger.setDestinationFileName(strDestPathFileName);
            pdfMerger.addSource(filePDFCover);
            pdfMerger.addSource(fileInput);
            pdfMerger.mergeDocuments(null);

            File fileDest = new File(strDestPathFileName);
            if (fileDest.exists()) {
                filePDFCover.delete();
                FileUtil.rename(fileDest, fileInput.getAbsolutePath(), true);
            }

            return fileInput;
        }
        return null;
    }

    /**
     * OFD文件中加入【封面】
     *
     * @param fileInput
     * @throws IOException
     */
    public File add2Ofd(File fileInput) throws IOException {
        if (this.getBase64() != null) {
            // 生成封面PDF文件
            File filePDFCover = getCoverPDF();
            // 转换为OFD版
            String strPdfPath = filePDFCover.getAbsolutePath();
            ConvertOfdUtil convertOfdUtil = new ConvertOfdUtil();
            File fileOFDCover = convertOfdUtil.convertPdf2Ofd(strPdfPath, strPdfPath + ".ofd");
            if (fileOFDCover.exists()) {
                filePDFCover.delete();
            }

            // 合成到现有OFD文件中，加到第一页。
            // 设置合并后的OFD文件的路径和文件名
            String strDestPathFileName = fileInput.getAbsolutePath() + ".ofd";
            File fileDest = new File(strDestPathFileName);
            // 声明OFD合并对象
            try (OFDMerger ofdMerger = new OFDMerger(fileDest.toPath())) {
                // 将需合并的文件加入“OFD合并对象”
                ofdMerger.add(fileOFDCover.toPath());
                ofdMerger.add(fileInput.toPath());
            }

            if (fileDest.exists()) {
                fileOFDCover.delete();
                FileUtil.rename(fileDest, fileInput.getAbsolutePath(), true);
            }

            return fileInput;
        }
        return null;
    }


}
