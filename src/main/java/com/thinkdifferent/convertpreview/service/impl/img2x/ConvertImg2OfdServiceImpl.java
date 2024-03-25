package com.thinkdifferent.convertpreview.service.impl.img2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.utils.ConvertOfdUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 18:02
 */
@Service
public class ConvertImg2OfdServiceImpl implements ConvertTypeService {
    @Resource
    private ConvertOfdUtil convertOfdUtil;
    @Resource(name = "convertImg2JpgServiceImpl")
    private ConvertImg2JpgServiceImpl convertImg2JpgService;

    /**
     * 图片 转 ofd, 需先将图片转为 jpg, 再转为 ofd
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        // 先转 jpg
        File jpgFile = convertImg2JpgService.convert(inputFile, targetDir + "_jpg");
        // 再转 ofd
        return convertOfdUtil.convertImg2Ofd(FileUtil.getCanonicalPath(jpgFile), targetDir + ".ofd");
    }
}
