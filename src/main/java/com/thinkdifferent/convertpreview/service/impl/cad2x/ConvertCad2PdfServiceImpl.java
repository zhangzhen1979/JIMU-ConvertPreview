package com.thinkdifferent.convertpreview.service.impl.cad2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.utils.Cad2PdfUtil;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2024/1/3 18:21
 */
@Service
public class ConvertCad2PdfServiceImpl implements ConvertTypeService {

    /**
     * 根据类型进行转换
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        String strPdfFile = targetDir + ".pdf";
        Assert.isTrue(Cad2PdfUtil.process(FileUtil.getCanonicalPath(inputFile), strPdfFile), "cad转pdf失败");
        return new File(strPdfFile);
    }
}
