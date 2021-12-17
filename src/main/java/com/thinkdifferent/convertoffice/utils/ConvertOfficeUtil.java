package com.thinkdifferent.convertoffice.utils;

import org.jodconverter.DocumentConverter;
import org.jodconverter.office.OfficeException;
import org.ofd.render.OFDRender;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.GeneralConvertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Order
@Component
public class ConvertOfficeUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConvertOfficeUtil.class);

    public static void main(String[] args) {
        ConvertOfficeUtil convertOfficeUtil = new ConvertOfficeUtil();
        convertOfficeUtil.convertOfd2Pdf("d:/cvtest/1-pdf.ofd", "d:/cvtest/1-pdf-1.pdf");
    }

    /**
     * 将输入文件转换为pdf
     * @param strInputFile 输入的文件路径和文件名
     * @param strOutputFile 输出的文件路径和文件名
     * @return
     */
    public File convertOffice2Pdf(String strInputFile, String strOutputFile) {
        File fileInput = new File(strInputFile);

        // 如果输出文件名不是pdf扩展名，加上
        if(!strOutputFile.endsWith(".pdf")){
            strOutputFile = strOutputFile + ".pdf";
        }
        File fileOutput = new File(strOutputFile);

        // 如果输入的文件存在，则执行转换
        if(fileInput.exists()){
            try {
                getDocumentConverter().convert(fileInput).to(fileOutput).execute();
            } catch (OfficeException e) {
                e.printStackTrace();
            }
        }else{
            // 如果输入的文件不存在，则返回空对象
            return null;
        }

        return fileOutput;
    }

    private DocumentConverter getDocumentConverter() {
        return SpringUtil.getClass(DocumentConverter.class);
    }

    /**
     * 将pdf文件转换为ofd文件
     * @param strPdfFilePath 输入的pdf文件路径和文件名
     * @param strOfdFilePath 输出的ofd文件路径和文件名
     * @return 返回的ofd文件的File对象
     */
    public File convertPdf2Ofd(String strPdfFilePath,String strOfdFilePath){
        Path pathPdfIn = Paths.get(strPdfFilePath);
        Path pathOfdOut = Paths.get(strOfdFilePath);
        try {
            logger.debug("开始转换PDF->OFD文件{}到{}",strPdfFilePath,strOfdFilePath);
            OFDRender.convertPdfToOfd(Files.newInputStream(pathPdfIn), Files.newOutputStream(pathOfdOut));
            return new File(strOfdFilePath);
        }catch (Exception e){
            logger.error("PDF文件转OFD异常:",e);
            return null;
        }
    }

    /**
     * OFD文件转换PDF文件
     * @param strOfdFilePath 输入的OFD文件
     * @param strPdfFilePath 输出的PDF文件
     * @return 转换后的PDF文件File对象
     */
    public File convertOfd2Pdf(String strOfdFilePath,String strPdfFilePath){
        // 1. 文件输入路径
        Path pathOfd = Paths.get(strOfdFilePath);
        // 2. 转换后文件输出位置
        Path pathPdf = Paths.get(strPdfFilePath);
        try {
            // 3. OFD转换PDF
            ConvertHelper.toPdf(pathOfd, pathPdf);
            logger.debug("生成文档位置: " + pathPdf.toAbsolutePath());
            return new File(strOfdFilePath);
        } catch (GeneralConvertException e) {
            // GeneralConvertException 类型错误表明转换过程中发生异常
            e.printStackTrace();
        }

        return null;
    }



}
