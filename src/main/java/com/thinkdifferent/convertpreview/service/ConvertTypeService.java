package com.thinkdifferent.convertpreview.service;

import cn.hutool.core.io.FileUtil;
import org.springframework.util.Assert;

import java.io.File;

/**
 * 各格式转换需实现该接口，实现类命名规则：ConvertImg2PdfServiceImpl..
 * 只进行同步转换，需异步转换功能需单独调用实现类
 *
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 17:48
 * @see com.thinkdifferent.convertpreview.consts.ConvertFileTypeEnum
 */
public interface ConvertTypeService {
    /**
     * 根据类型进行转换， 外部调用方法
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    default File convert(File inputFile, String targetDir){
        File file = convert0(inputFile, targetDir);
        Assert.isTrue(FileUtil.exist(file), "文件转换失败");
        return file;
    }

    /**
     * 根据类型进行转换, 内部实现，不建议直接调用
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    File convert0(File inputFile, String targetDir);
}
