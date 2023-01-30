package com.thinkdifferent.convertpreview.utils.convert4pdf;

import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import com.thinkdifferent.convertpreview.utils.SpringUtil;
import lombok.extern.log4j.Log4j2;
import org.jodconverter.DocumentConverter;
import org.jodconverter.office.OfficeException;

import java.io.File;

@Log4j2
public class PdfByLibreOffice extends ConvertPdf {

    @Override
    public File convert(Object objInputFile, String strOutputFile, ConvertEntity convertEntity) throws OfficeException {
        if(objInputFile != null){
            String strInputFile = String.valueOf(objInputFile);
            File fileInput = new File(strInputFile);
            File fileOutput = new File(strOutputFile);
            getDocumentConverter().convert(fileInput).to(fileOutput).execute();

            return fileOutput;
        }
        return null;
     }


    private DocumentConverter getDocumentConverter() {
        return SpringUtil.getClass(DocumentConverter.class);
    }


    /**
     * 是否匹配
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        return "libre".equalsIgnoreCase(input);
    }


}
