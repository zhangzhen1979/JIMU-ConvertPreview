package com.thinkdifferent.convertpreview.entity;

import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.entity.writeback.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Objects;

/**
 * 文件回写方式
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/13 15:37
 */
public enum WriteBackType {
    // 路径回写
    PATH(WriteBackPath.class),
    // 服务端路径回写
    LOCAL(WriteBackPath.class),
    // 接口回写
    URL(WriteBackUrl.class),
    // FTP 回写
    FTP(WriteBackFtp.class),
    ;

    private final Class<? extends WriteBack> clazzWriteBack;

    WriteBackType(Class<? extends WriteBack> clazzWriteBack) {
        this.clazzWriteBack = clazzWriteBack;
    }

    /**
     * 转换回写对象
     *
     * @param parameters 传入数据
     * @return wb
     */
    public WriteBack convert(Map<String, Object> parameters) throws IllegalAccessException, InstantiationException {
        Object writeBackParams = parameters.get("writeBack");
        if (Objects.isNull(writeBackParams)) {
            return new WriteBackNone();
        }
        Assert.isTrue(writeBackParams instanceof Map, "回写信息格式错误");

        WriteBack writeBack = this.clazzWriteBack.newInstance().of((Map<String, Object>) writeBackParams);

        if (LOCAL.equals(this)) {
            // 服务端路径回写，添加服务端输出文件夹
            ((WriteBackPath) writeBack).setPath(ConvertConfig.outPutPath);
        }

        if (URL.equals(this)) {
            // URL 请求添加请求头
            ((WriteBackUrl) writeBack).setWriteBackHeaders((Map) parameters.get("writeBackHeaders"));
        }

        return writeBack;
    }

    /**
     * @param writeBack 输入的回调地址
     * @return 输入对象
     */
    public static WriteBack of(String writeBack) throws IllegalAccessException, InstantiationException {
        if (StringUtils.isBlank(writeBack)) {
            return new WriteBackNone();
        }

        for (WriteBackType writeBackEnums : WriteBackType.values()) {
            WriteBack writeBackVal = writeBackEnums.clazzWriteBack.newInstance();
            if (writeBackVal.match(writeBack)) {
                return writeBackVal;
            }
        }

        throw new RuntimeException("no match writeBack :" + writeBack);
    }
}
