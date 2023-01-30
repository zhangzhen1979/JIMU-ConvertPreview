package com.thinkdifferent.convertpreview.utils.convert4pdf;

import cn.hutool.core.io.FileUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import com.lowagie.text.pdf.codec.TiffImage;
import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import com.thinkdifferent.convertpreview.utils.convert4jpg.ConvertJpgEnum;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

@Log4j2
public class Tiff2Pdf extends ConvertPdf {

    @Override
    public File convert(Object objInputFile, String strOutputFile, ConvertEntity convertEntity)
            throws Exception{
        if(objInputFile != null) {
            String strTifFile = String.valueOf(objInputFile);
            @Cleanup RandomAccessFileOrArray rafa = new RandomAccessFileOrArray(strTifFile);
            @Cleanup Document document = new Document();
            // 设置文档页边距
            document.setMargins(0, 0, 0, 0);

            PdfWriter.getInstance(document, new FileOutputStream(strOutputFile));
            document.open();
            int intPages = TiffImage.getNumberOfPages(rafa);

            if (intPages == 1) {
                String strJpg = strTifFile.substring(0, strTifFile.lastIndexOf(".")) + ".jpg";
                File fileJpg = new File(strJpg);
                List<String> listPic2Jpg = ConvertJpgEnum.convert(strTifFile, strJpg);

                if (fileJpg.exists()) {
                    Jpg2Pdf jpg2Pdf = new Jpg2Pdf();
                    if (FileUtil.exist(jpg2Pdf.convert(listPic2Jpg, strOutputFile, convertEntity))) {
                        FileUtil.del(fileJpg);
                    }
                }
            } else {
                Image image;
                for (int i = 1; i <= intPages; i++) {
                    image = TiffImage.getTiffImage(rafa, i);
                    // 设置页面宽高与图片一致
                    Rectangle pageSize = new Rectangle(image.getScaledWidth(), image.getScaledHeight());
                    document.setPageSize(pageSize);
                    // 图片居中
                    image.setAlignment(Element.ALIGN_CENTER);
                    //新建一页添加图片
                    document.newPage();
                    document.add(image);
                }
            }

            return new File(strOutputFile);
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
        return "tif".equalsIgnoreCase(input) || "tiff".equalsIgnoreCase(input);
    }

}
