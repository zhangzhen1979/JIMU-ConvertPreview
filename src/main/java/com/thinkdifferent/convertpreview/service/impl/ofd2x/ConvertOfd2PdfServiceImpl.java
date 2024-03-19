package com.thinkdifferent.convertpreview.service.impl.ofd2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.utils.ConvertOfdUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 18:02
 */
@Service
public class ConvertOfd2PdfServiceImpl implements ConvertTypeService {
    @Resource
    private ConvertOfdUtil convertOfdUtil;

    /**
     * ofd 2 pdf
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @Override
    public File convert0(File inputFile, String targetDir) {
        return convertOfdUtil.convertOfd2Pdf(FileUtil.getCanonicalPath(inputFile), targetDir + ".pdf");
    }
}
