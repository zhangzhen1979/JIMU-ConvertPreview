package com.thinkdifferent.convertpreview.entity.writeback;

import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 回写父类
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 10:14
 */
public abstract class WriteBack {
    /**
     * 是否匹配
     * @param input 输入内容
     * @return 是否匹配
     */
    public abstract boolean match(String input);
    /**
     * 对象转换
     *
     * @param writeBack 传入的完整参数map
     * @return wb
     */
    public abstract WriteBack of(Map<String, Object> writeBack);

    /**
     * @return jpg文件输出路径
     */
    public String getOutputPath() {
        return ConvertConfig.outPutPath;
    }

    /**
     * 转换结果回写
     *
     * @param outPutFileType 目标文件类型
     * @param fileOut        转换后的文件
     * @param listJpg        转换后的jpg文件
     */
    public abstract WriteBackResult writeBack(String outPutFileType, File fileOut, List<String> listJpg);
}
