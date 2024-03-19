package com.thinkdifferent.convertpreview.service.impl.engine;

import cn.hutool.extra.spring.SpringUtil;
import com.thinkdifferent.convertpreview.engine.EngineService;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Objects;

/**
 * 引擎处理，即默认处理，所有标准功能不支持的转换，都走引擎处理
 *
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 18:02
 */
@Service
public class ConvertEngine2OfdServiceImpl implements ConvertTypeService {
    private EngineService engineService;

    /**
     * 引擎 转 ofd
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        if (Objects.isNull(engineService)) {
            engineService = SpringUtil.getBean(EngineService.class);
        }
        return engineService.convertOfd(inputFile, targetDir + ".ofd");
    }
}
