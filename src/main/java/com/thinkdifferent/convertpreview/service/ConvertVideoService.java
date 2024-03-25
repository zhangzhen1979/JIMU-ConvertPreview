package com.thinkdifferent.convertpreview.service;

import com.thinkdifferent.convertpreview.entity.ConvertVideoEntity;
import com.thinkdifferent.convertpreview.entity.TargetFile;

import java.io.IOException;

public interface ConvertVideoService {

    /**
     * 音视频文件转换
     *
     * @param convertVideoEntity 视频对象
     * @param type  模式标识：当前只有 preview
     * @return 转换后文件
     */
    TargetFile convert(ConvertVideoEntity convertVideoEntity, String type) throws IOException;
}
