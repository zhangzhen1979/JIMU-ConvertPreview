package com.thinkdifferent.convertpreview.entity.input;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.thinkdifferent.convertpreview.cache.CacheManager;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import org.apache.commons.lang3.StringUtils;
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
     * 缓存
     */
    private CacheManager cacheManager;

    /**
     * 单页文件是否补空白页
     */
    private boolean duplexPrint = false;
    public boolean getDuplexPrint(){
        return duplexPrint;
    }

    /**
     * 补充的空白页是否添加页码
     */
    private boolean blankPageHaveNum = false;
    public boolean getBlankPageHaveNum(){
        return blankPageHaveNum;
    }

    /**
     * 检测输入字符串是否满足该输入类型, 只匹配，不做文件存在校验
     *
     * @param inputStr 输入的字符串
     * @return bln
     */
    public abstract boolean matchInput(String inputStr);

    /**
     * 输入字符串转换成对应类
     *
     * @param inputPath   文件路径
     * @param strFileName 文件名
     * @param strExt      文件扩展名
     * @return input对象
     */
    public abstract Input of(String inputPath, String strFileName, String strExt);

    /**
     * 输入字符串转换成对应类
     *
     * @param inputPath          文件路径
     * @param strFileName        文件名
     * @param strExt             文件扩展名
     * @param duplexPrint        单页文件是否补空白页
     * @param blankPageHaveNum   补充的空白页是否添加页码
     * @return input对象
     */
    public Input of(String inputPath, String strFileName, String strExt,
                    boolean duplexPrint, boolean blankPageHaveNum){
        Input input = this.of(inputPath, strFileName,strExt);
        input.duplexPrint = duplexPrint;
        input.blankPageHaveNum = blankPageHaveNum;
        return input;
    }

    /**
     * 判断传入资源是否存在
     *
     * @return bln
     */
    public abstract boolean exists();

    /**
     * 获取文件
     *
     * @return file
     */
    public abstract File getInputFile();

    public File checkAndGetInputFile() {
        File file = getInputFile();
        Assert.notNull(file, "获取文件为空");
        return file;
    }

    protected void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }


    /**
     * @return 获取【输入文件临时文件夹】
     */
    protected String getInputTempPath() {
        return SystemUtil.beautifulPath(ConvertDocConfigBase.inPutTempPath);
    }

    /**
     * 获取缓存中记录的已下载的文件【文件已下载且存在】
     *
     * @param key cache k
     * @return 已下载的文件
     */
    protected File getCacheFile(String key) {
        if (Objects.isNull(cacheManager)) {
            try {
                cacheManager = SpringUtil.getBean(CacheManager.class);
            } catch (Exception | Error ignored) {
            }
        }

        String cacheFilePath;
        if (Objects.nonNull(cacheManager)
                && StringUtils.isNotBlank(cacheFilePath = cacheManager.getString(key))
                && FileUtil.exist(cacheFilePath)) {
            return new File(cacheFilePath);
        }
        return null;
    }

    /**
     * 添加文件地址缓存
     *
     * @param key      cache key
     * @param filePath 文件路径
     */
    protected void addCache(String key, String filePath) {
        if (Objects.isNull(cacheManager)) {
            try {
                cacheManager = SpringUtil.getBean(CacheManager.class);
            } catch (Exception | Error ignored) {
            }
        }
        if (Objects.nonNull(cacheManager)) {
            cacheManager.set(key, filePath);
        }
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
