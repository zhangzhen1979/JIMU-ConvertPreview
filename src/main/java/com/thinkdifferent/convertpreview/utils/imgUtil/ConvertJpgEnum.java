package com.thinkdifferent.convertpreview.utils.imgUtil;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 图片转换方式
 *
 * @author 张镇
 * @version 1.0
 * @date 2023-1-30 11:31:21
 */
public enum ConvertJpgEnum {
    // ImgUtil可处理格式
    JPG(ImgUtil2Jpg.class),
    JPEG(ImgUtil2Jpg.class),
    // 其他常见图片格式
    GIF(Twelemonkey2Jpg.class),
    TIF(Twelemonkey2Jpg.class),
    IMG(Twelemonkey2Jpg.class),
    ;

    private final Class<? extends ConvertJpg> clazzConvertJpg;

    ConvertJpgEnum(Class<? extends ConvertJpg> clazzConvertJpg) {
        this.clazzConvertJpg = clazzConvertJpg;
    }

    /**
     * 转换图片为JPG
     *
     * @param strInputFile     输入文件路径
     * @param strOutputFile    输出文件路径
     * @return List<String>    转换后的JPG文件路径List
     */
    public static List<String> convert(String strInputFile, String strOutputFile) throws IllegalAccessException, InstantiationException, IOException {
        Assert.isTrue(StringUtils.isNotBlank(strInputFile), "文件名为空");
        File inputFile = new File(strInputFile);
        Assert.isTrue(inputFile.exists(), "找不到文件【" + strInputFile + "】");
        strInputFile = SystemUtil.beautifulFilePath(strInputFile);
        strOutputFile = SystemUtil.beautifulFilePath(strOutputFile);
        String strFileExt = FileUtil.extName(inputFile).toUpperCase();
        if(inputFile.length() > 0) {
            String strJpgPath = strOutputFile.substring(0, strOutputFile.lastIndexOf("/"));
            // 处理目标文件夹，如果不存在则自动创建
            File fileJpgPath = new File(strJpgPath);
            if (!fileJpgPath.exists()) {
                fileJpgPath.mkdirs();
            }

            for (ConvertJpgEnum convertJpgEnum : ConvertJpgEnum.values()) {
                ConvertJpg convertJpgVal = convertJpgEnum.clazzConvertJpg.newInstance();
                if (convertJpgVal.match(strFileExt)) {
                    return convertJpgVal.convert(strInputFile, strOutputFile);
                }
            }
        }

        throw new RuntimeException("暂不支持的图片转换格式：" + strFileExt);
    }
}
