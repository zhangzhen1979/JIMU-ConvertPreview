package com.thinkdifferent.convertpreview.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * 转换结果对象
 *
 * @author ltian
 * @version 1.0
 * @date 2023/11/30 14:58
 */
@Data
@Accessors(chain = true)
public class ConvertResult implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 转换结果
     */
    private boolean blnFlag;
    /**
     * 单文件转换结果
     */
    private File resultFile;
    /**
     * 多文件转换结果
     */
    private List<File> resultFiles;
}
