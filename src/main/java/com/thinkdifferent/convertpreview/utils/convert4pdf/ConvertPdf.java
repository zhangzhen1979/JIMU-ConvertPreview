package com.thinkdifferent.convertpreview.utils.convert4pdf;

import com.thinkdifferent.convertpreview.entity.ConvertEntity;

import java.io.File;

public abstract class ConvertPdf {

    /**
     * 将输入的文件（文档、图片）转换为Pdf文件。
     *
     * @param objInputFile  输入文件的路径和文件名（如果输入的是JPG，则允许传入List<String>）
     * @param strOutputFile 输出文件的路径和文件名
     * @param convertEntity 转换参数
     * @return              PDF文件的File对象
     */
    public abstract File convert(Object objInputFile, String strOutputFile,
                                 ConvertEntity convertEntity)
            throws Exception;

    /**
     * 是否匹配
     * @param input 输入内容
     * @return 是否匹配
     */
    public abstract boolean match(String input);

}
