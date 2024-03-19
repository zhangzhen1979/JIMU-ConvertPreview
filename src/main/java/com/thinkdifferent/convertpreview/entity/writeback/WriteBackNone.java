package com.thinkdifferent.convertpreview.entity.writeback;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.entity.ZipParam;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 不需要回写
 * @author ltian
 * @version 1.0
 * @date 2022/7/26 10:36
 */
public class WriteBackNone extends WriteBack {

    public WriteBackNone() {
    }

    /**
     * 是否匹配, 不需要回写不匹配，手动指定
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        return false;
    }

    /**
     * 对象转换
     *
     * @param writeBack 传入的完整参数map
     * @return wb
     */
    @Override
    public WriteBack of(Map<String, Object> writeBack) {
        return new WriteBackNone();
    }

    /**
     * 转换结果回写
     *
     * @param outPutFileType 目标文件类型
     * @param fileOut        转换后的文件
     * @param listJpg        转换后的jpg文件
     * @param zipParam       zip参数
     */
    @Override
    public WriteBackResult writeBack(String outPutFileType, File fileOut, List<String> listJpg, ZipParam zipParam) {
        return new WriteBackResult(true).setFile(FileUtil.getCanonicalPath(fileOut));
    }
}
