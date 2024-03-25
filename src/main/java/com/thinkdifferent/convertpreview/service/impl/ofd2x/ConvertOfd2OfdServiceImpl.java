package com.thinkdifferent.convertpreview.service.impl.ofd2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 18:02
 */
@Service
public class ConvertOfd2OfdServiceImpl implements ConvertTypeService {
    /**
     * ofd 2 ofd
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @Override
    public File convert0(File inputFile, String targetDir) {
        File targetFile = new File(targetDir + ".ofd");
        if(!inputFile.equals(targetFile)){
            FileUtil.copyFile(inputFile, targetFile);
        }
        return targetFile;
    }
}
