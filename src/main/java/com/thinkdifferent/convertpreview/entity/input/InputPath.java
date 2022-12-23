package com.thinkdifferent.convertpreview.entity.input;


import cn.hutool.core.io.FileUtil;

import java.io.File;

/**
 * 本地文件路径输入
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 11:03
 */
public class InputPath extends Input {
    /**
     * 本地文件路径
     */
    private String filePath;

    @Override
    public boolean matchInput(String inputStr) {
        return new File(inputStr).exists() && new File(inputStr).isFile();
    }

    @Override
    public Input of(String inputPath, String strExt) {
        InputPath path = new InputPath();
        path.setFilePath(inputPath);
        return path;
    }

    /**
     * 判断传入资源是否存在
     *
     * @return bln
     */
    @Override
    public boolean exists() {
        return new File(filePath).exists();
    }

    @Override
    public File getInputFile() {
        if (super.inputFile == null) {
            super.setInputFile(new File(filePath));
        }
        return super.inputFile;
    }


    /**
     * 本地原路径不需要处理原文件，只清理转换后的图片
     */
    @Override
    public void clean() {
        // 文件临时存储路径
        File imageFile = new File(getBaseUrl());
        FileUtil.del(imageFile);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
