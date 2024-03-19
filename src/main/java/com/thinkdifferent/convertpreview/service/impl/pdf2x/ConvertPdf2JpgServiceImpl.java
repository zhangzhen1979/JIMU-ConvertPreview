package com.thinkdifferent.convertpreview.service.impl.pdf2x;

import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.utils.pdfUtil.ConvertPdfUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 18:02
 */
@Service
public class ConvertPdf2JpgServiceImpl implements ConvertTypeService {
    @Resource
    private ConvertPdfUtil convertPdfUtil;

    /**
     * pdf转图片
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @Override
    public File convert0(File inputFile, String targetDir) {
        convertPdfUtil.pdf2jpg(inputFile, targetDir, null, false);
        // 异步转换不一定能完 FileUtil.del(inputFile);
        return new File(targetDir);
    }
}
