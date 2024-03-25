package com.thinkdifferent.convertpreview.service.impl.img2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.utils.imgUtil.ConvertJpgEnum;
import com.thinkdifferent.convertpreview.utils.imgUtil.JpgUtil;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 18:02
 */
@Service
public class ConvertImg2JpgServiceImpl implements ConvertTypeService {
    /**
     * 图片转图片
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        // 转换
        String strInputExt = FileUtil.extName(inputFile);
        if("jpg".equalsIgnoreCase(strInputExt) ||
                "jpeg".equalsIgnoreCase(strInputExt)){
            FileUtil.copy(inputFile, new File(targetDir+".jpg"), true);
        }else{
            ConvertJpgEnum.convert(FileUtil.getCanonicalPath(inputFile), targetDir+".jpg");
        }

        String strInputFilePath = targetDir+".jpg";
        File fileJpg = new File(strInputFilePath);
        if(fileJpg.exists()){
            // 压缩
            if(ConvertDocConfigBase.picQuality != 1
                    && ConvertDocConfigBase.picQuality != 0){
                if(fileJpg.isFile()){
                    thumbnail(strInputFilePath);
                }else{
                    // 遍历此文件夹下所有文件，转换。
                    File[] files = fileJpg.listFiles();
                    for(int i=0;i<files.length;i++){
                        thumbnail(files[i].getAbsolutePath());
                    }
                }
            }

            return fileJpg;
        }else{
            return null;
        }
    }


    private void thumbnail(String strInputFilePath) throws Exception {
        File fileJpg = new File(strInputFilePath);
        String strOutputFilePath = strInputFilePath + "_quality.jpg";

        JpgUtil jpgUtil = new JpgUtil();
        File fileJpgNew = jpgUtil.thumbnail(strInputFilePath, strOutputFilePath,
                1, ConvertDocConfigBase.picQuality);
        FileUtil.del(fileJpg);
        FileUtil.move(fileJpgNew, fileJpg, true);
    }

}
