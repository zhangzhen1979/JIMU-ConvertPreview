package com.thinkdifferent.convertpreview.utils.convert4pdf;

import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;

@Log4j2
public class PdfByLocalUtil extends ConvertPdf {

    @Override
    public File convert(Object objInputFile, String strOutputFile, ConvertEntity convertEntity)
            throws IOException, InterruptedException {
        if(objInputFile != null){
            String strInputFile = String.valueOf(objInputFile);
            // 开始时间
            long stime = System.currentTimeMillis();

            boolean blnFlag = LocalConvertUtil.process(strInputFile, strOutputFile);

            // 结束时间
            long etime = System.currentTimeMillis();
            // 计算执行时间
            if(blnFlag){
                log.info("Convert PDF success, Use time is: " + (int)((etime - stime)/1000) + " s...");
                return new File(strOutputFile);
            }else{
                log.info("Convert PDF fail, Use time is: " + (int)((etime - stime)/1000) + " s...");
                return null;
            }
        }

        return null;
    }


    /**
     * 是否匹配
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        return "wps".equalsIgnoreCase(input) || "office".equalsIgnoreCase(input);
    }

}
