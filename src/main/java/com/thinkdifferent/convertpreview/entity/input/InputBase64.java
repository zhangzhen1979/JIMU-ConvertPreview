package com.thinkdifferent.convertpreview.entity.input;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

/**
 * web地址
 *
 * @author 张镇
 * @version 1.0
 * @date 2023/4/14 17:57
 */
@Data
@Log4j2
public class InputBase64 extends Input {
    /**
     * 需转换的输入文件在Web服务中的URL地址
     */
    private String base64;
    private String fileName;
    private String fileType;

    public boolean matchInput(String inputStr) {
        return StringUtils.startsWithAny(inputStr, "data:application/pdf;base64,");
    }

    @Override
    public Input of(String strBase64, String strFileName, String strExt) {
        InputBase64 base64 = new InputBase64();
        base64.setBase64(strBase64);
        base64.setFileName(strFileName);
        base64.setFileType(strExt);
        return base64;
    }

    /**
     * 判断传入资源是否存在
     *
     * @return bln
     */
    @Override
    public boolean exists() {
        File file = getInputFile();
        return Objects.nonNull(file) && file.exists();
    }

    protected String getOutputPath() {
        return SystemUtil.beautifulPath(ConvertDocConfigBase.outPutPath);
    }


    @Override
    public File getInputFile() {
        // 文件临时存储路径
        if (super.inputFile == null) {
            // 判断缓存中是否存在
            super.inputFile = super.getCacheFile(this.base64);
            if (super.inputFile == null) {
                if (fileName == null
                        || fileName.equalsIgnoreCase("null")
                        || fileName.isEmpty()) {
                    fileName = UUID.randomUUID() + "." + this.getFileType();
                } else {
                    fileName = fileName + "." + this.getFileType();
                }
                if (!StringUtils.isEmpty(fileName)) {
                    // 判断文件是否存在
                    String downloadFilePath = super.getInputTempPath() + fileName;
                    File fileDownload = new File(downloadFilePath);
                    if (fileDownload != null &&
                            fileDownload.exists()) {
                        FileUtil.del(downloadFilePath);
                    }

                    log.debug("base64:" + base64 + " ;downloadFilePath:" + downloadFilePath);

                    if (base64.contains("data:application/pdf;base64,")) {
                        base64 = base64.substring(28);
                    }

                    com.thinkdifferent.convertpreview.utils.FileUtil.base64ToFile(base64,
                            super.getInputTempPath(), fileName);
                    Assert.isTrue(FileUtil.exist(downloadFilePath), "Base64生成文件失败");
                    super.setInputFile(fileDownload);
                    super.addCache(downloadFilePath, this.base64);

                }
            }
        }
        return super.inputFile;
    }

}
