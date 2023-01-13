package com.thinkdifferent.convertpreview.entity.input;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Objects;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 11:03
 */
public abstract class Input {
    /**
     * 传入的文件对象
     */
    protected File inputFile;
    /**
     * 检测输入字符串是否满足该输入类型, 只匹配，不做文件存在校验
     *
     * @param inputStr 输入的字符串
     * @return bln
     */
    public abstract boolean matchInput(String inputStr);

    /**
     * 输入字符串转换成对应类
     * @param inputPath 文件路径
     * @param strExt 文件扩展名
     * @return  input对象
     */
    public abstract Input of(String inputPath, String strExt);

    /**
     * 判断传入资源是否存在
     * @return  bln
     */
    public abstract boolean exists();

    /**
     * 获取文件
     * @return file
     */
    public abstract File getInputFile();

    public File checkAndGetInputFile(){
        File file = getInputFile();
        Assert.notNull(file, "获取文件为空");
        return file;
    }

    protected void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }


    /**
     * @return 父级目录
     */
    protected String getBaseUrl() {
        return SystemUtil.beautifulDir(ConvertConfig.inPutTempPath);
    }

    /**
     * 清理文件, 下载文件及转换后的图片
     * PATH 单独处理
     */
    public void clean() {
        if (Objects.nonNull(inputFile) && inputFile.isFile() && inputFile.exists()) {
            FileUtil.del(inputFile);
        }
    }
}
