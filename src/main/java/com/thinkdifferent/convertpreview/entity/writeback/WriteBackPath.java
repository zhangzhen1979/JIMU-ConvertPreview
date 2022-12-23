package com.thinkdifferent.convertpreview.entity.writeback;

import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.utils.SystemUtil;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 10:14
 */
public class WriteBackPath extends WriteBack {
    /**
     * 回写路径
     */
    private String path;

    @Override
    public WriteBack of(Map<String, Object> writeBack) {
        WriteBackPath writeBackPath = new WriteBackPath();
        writeBackPath.setPath(MapUtil.getStr(writeBack, "path"));
        return writeBackPath;
    }

    @Override
    public String getOutputPath() {
        // 本地路径使用配置的输出路径
        return SystemUtil.beautifulDir(path);
    }

    /**
     * 是否匹配
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        if (new File(input).exists()){
            this.setPath(input);
            return true;
        }else{
            return false;
        }
    }

    /**
     * 转换结果回写
     *
     * @param outPutFileType 目标文件类型
     * @param fileOut        转换后的文件
     * @param listJpg        转换后的jpg文件
     */
    @Override
    public WriteBackResult writeBack(String outPutFileType, File fileOut, List<String> listJpg) {
        return new WriteBackResult(true);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
