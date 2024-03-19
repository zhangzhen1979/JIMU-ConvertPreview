package com.thinkdifferent.convertpreview.service;

import com.thinkdifferent.convertpreview.entity.ConvertDocEntity;
import com.thinkdifferent.convertpreview.entity.TargetFile;

import java.io.IOException;

public interface ConvertDocService {
    /**
     * 文档转换
     *
     * @param convertDocEntity 转换对象
     * @return 转换后文件或父级文件目录
     */
    TargetFile convert(ConvertDocEntity convertDocEntity) throws IOException;

    /**
     * 根据文件名，删除临时文件夹中的临时文件
     *
     * @param strFileName 临时文件名
     * @return 删除是否成功
     */
    boolean deleteTempFile(String strFileName);
}
