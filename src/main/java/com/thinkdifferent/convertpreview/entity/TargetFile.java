package com.thinkdifferent.convertpreview.entity;

import cn.hutool.core.io.FileUtil;
import lombok.Data;

import java.io.File;

@Data
public class TargetFile {

    public TargetFile() {
        this.noCheck = true;
    }

    private File target;

    private long longPageCount = 0;

    private boolean noCheck;

    public void setTarget(File target) {
        this.target = target;
        this.noCheck = "m3u8".equalsIgnoreCase(FileUtil.extName(target));
    }

    /**
     * 兼容m3u8不判断是否存在逻辑
     * @return bln
     */
    public boolean check(){
        return noCheck || FileUtil.exist(target);
    }
}
