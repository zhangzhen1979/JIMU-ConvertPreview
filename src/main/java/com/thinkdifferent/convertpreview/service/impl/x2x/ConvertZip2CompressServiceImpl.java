package com.thinkdifferent.convertpreview.service.impl.x2x;

import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.utils.ArcZipUtil;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 压缩文件 解压， 支持 zip\rar\7z
 *
 * @author ltian
 * @version 1.0
 * @date 2024/1/11 11:15
 */
@Service
public class ConvertZip2CompressServiceImpl implements ConvertTypeService {
    /**
     * 根据类型进行转换, 内部实现，不建议直接调用
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
//        File targetFile = FileUtil.file(targetDir, inputFile.getName());
//        ArcZipUtil.unzip(inputFile, targetFile.getCanonicalPath(), null);
        return ArcZipUtil.treeFile(inputFile, "");
    }
}
