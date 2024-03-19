package com.thinkdifferent.convertpreview.entity.input;


import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.Data;

import java.io.File;

/**
 * 本地文件路径输入
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 11:03
 */
@Data
public class InputPath extends Input {
    /**
     * 本地文件路径
     */
    private String filePath;

    @Override
    public boolean matchInput(String inputStr) {
        return new File(inputStr).exists() && new File(inputStr).isFile();
    }

    @Override
    public Input of(String inputPath, String strFileName, String strExt) {
        InputPath path = new InputPath();
        path.setFilePath(SystemUtil.beautifulFilePath(inputPath));
        return path;
    }

    /**
     * 判断传入资源是否存在
     *
     * @return bln
     */
    @Override
    public boolean exists() {
        return FileUtil.exist(filePath);
    }

    @Override
    public File getInputFile() {
        if (super.inputFile == null) {
            // 将文件复制到intemp文件夹
            String strFileName = FileUtil.getName(filePath);
            File fileTarget = new File(ConvertDocConfigBase.inPutTempPath + strFileName);
            if (!FileUtil.equals(new File(filePath), fileTarget)) {
                if (fileTarget.exists()) {
                    FileUtil.del(fileTarget);
                }
                FileUtil.copy(new File(filePath), fileTarget, true);
            }
            super.setInputFile(fileTarget);
        }
        return super.inputFile;
    }


    /**
     * 本地原路径不需要处理原文件，只清理转换后的图片
     */
    @Override
    public void clean() {
        // 文件临时存储路径
        File fileTemp = new File(getInputTempPath());
        FileUtil.del(fileTemp);
    }

}
