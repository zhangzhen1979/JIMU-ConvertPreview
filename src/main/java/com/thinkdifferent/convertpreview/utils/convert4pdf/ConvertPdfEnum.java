package com.thinkdifferent.convertpreview.utils.convert4pdf;

import com.thinkdifferent.convertpreview.entity.ConvertEntity;

import java.io.File;

/**
 * 图片转换方式
 *
 * @author 张镇
 * @version 1.0
 * @date 2023-1-30 11:31:21
 */
public enum ConvertPdfEnum {
    // JPG转PDF
    JPG(Jpg2Pdf.class),
    // TIF转PDF
    TIF(Tiff2Pdf.class),
    // 本地WPS转PDF
    WPS(PdfByLocalUtil.class),
    // 本地Office转PDF
    OFFICE(PdfByLocalUtil.class),
    // LibreOffice转PDF
    LIBRE(PdfByLibreOffice.class),
    ;

    private final Class<? extends ConvertPdf> clazzConvertPdf;

    ConvertPdfEnum(Class<? extends ConvertPdf> clazzConvertPdf) {
        this.clazzConvertPdf = clazzConvertPdf;
    }

    /**
     * 文件转换为PDF
     *
     * @param strType       输入类型。包括图片文件扩展名（jpg、tif）、转换引擎名称
     * @param objInputFile  输入文件路径
     * @param strOutputFile 输出文件路径
     * @return List<String>    转换后的JPG文件路径List
     */
    public static File convert(String strType, Object objInputFile, String strOutputFile,
                               ConvertEntity convertEntity)
            throws Exception {
        for (ConvertPdfEnum convertPdfEnum : ConvertPdfEnum.values()) {
            ConvertPdf convertPdfVal = convertPdfEnum.clazzConvertPdf.newInstance();

            if (convertPdfVal.match(strType)) {
                return convertPdfVal.convert(objInputFile, strOutputFile, convertEntity);
            }
        }

        return null;
    }

}
