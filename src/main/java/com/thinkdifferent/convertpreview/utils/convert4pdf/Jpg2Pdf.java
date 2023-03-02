package com.thinkdifferent.convertpreview.utils.convert4pdf;

import cn.hutool.core.io.FileUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

@Log4j2
public class Jpg2Pdf extends ConvertPdf {

    @Override
    public File convert(Object objInputFile, String strOutputFile, ConvertEntity convertEntity) {

        if (objInputFile != null) {
            List<String> listJpgFile = (List<String>) objInputFile;
            log.info("jpg {} 转pdf {}", String.join(" ", listJpgFile), strOutputFile);
            Document document = null;
            FileOutputStream fos = null;
            File filePdf = new File(strOutputFile);

            try {
                document = new Document();

                // 设置文档页边距
                document.setMargins(0, 0, 0, 0);
                fos = new FileOutputStream(strOutputFile);
                PdfWriter.getInstance(document, fos);
                // 打开文档
                document.open();

                // 循环，读取每个文件，添加到pdf的document中。
                for (String strJpgFile : listJpgFile) {
                    File fileJpg = new File(strJpgFile);
                    if(fileJpg != null && fileJpg.exists() && fileJpg.length() > 0){
                        try {
                            // 获取图片的宽高
                            Image image = Image.getInstance(strJpgFile);
                            float floatImageHeight = image.getScaledHeight();
                            float floatImageWidth = image.getScaledWidth();
                            // 设置页面宽高与图片一致
                            Rectangle rectangle = new Rectangle(floatImageWidth, floatImageHeight);
                            document.setPageSize(rectangle);
                            // 图片居中
                            image.setAlignment(Element.ALIGN_CENTER);
                            //新建一页添加图片
                            document.newPage();
                            document.add(image);
                        } catch (Exception imgExp) {
                            log.error(imgExp);
                            continue;
                        }
                    }
                }

                fos.flush();

            } catch (Exception e) {
                log.error(e);
            } finally {
                try {
                    if (document != null && document.isOpen()) {
                        document.close();
                    }
                } catch (Exception e) {
                    log.error(e);
                }

                try {
                    if (fos != null) {
                        fos.close();
                    }
                    if (filePdf.length() == 0) {
                        FileUtil.del(new File(strOutputFile));
                    }
                } catch (Exception e) {
                    log.error(e);
                }

            }
            return filePdf;
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
        return "jpg".equalsIgnoreCase(input);
    }


}
