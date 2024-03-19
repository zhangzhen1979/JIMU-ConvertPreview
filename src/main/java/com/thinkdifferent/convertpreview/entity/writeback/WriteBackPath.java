package com.thinkdifferent.convertpreview.entity.writeback;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.entity.ZipParam;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.Data;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 10:14
 */
@Data
public class WriteBackPath extends WriteBack {
    /**
     * 回写路径
     */
    private String path;

    @Override
    public WriteBack of(Map<String, Object> writeBack) {
        WriteBackPath writeBackPath = new WriteBackPath();
        writeBackPath.setPath(MapUtil.getStr(writeBack, "path"));
        return writeBackPath;
    }

    @Override
    public String getOutputPath() {
        // 本地路径使用配置的输出路径
        return SystemUtil.beautifulPath(path);
    }

    /**
     * 是否匹配
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        if (new File(input).exists()) {
            this.setPath(input);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 转换结果回写
     *
     * @param outPutFileType 目标文件类型
     * @param fileOut        转换后的文件
     * @param listJpg        转换后的jpg文件
     * @param zipParam       zip压缩参数
     */
    @Override
    public WriteBackResult writeBack(String outPutFileType, File fileOut, List<String> listJpg, ZipParam zipParam)
            throws ZipException {
        WriteBackResult writeBackResult = new WriteBackResult();

        if (fileOut.exists()) {
            Map<String, Object> mapZip = getZip(fileOut, zipParam);
            // 判断是否需要进行zip压缩
            if (mapZip != null && mapZip.get("flag").equals(true)) {
                writeBackResult.setTempFile(fileOut.getAbsolutePath());
                String strFile = mapZip.get("fileDest").toString();
                fileOut = new File(strFile);
            }
            writeBackResult.setFile(fileOut.getAbsolutePath());
            if (mapZip.containsKey("pwd")) {
                writeBackResult.setZippwd((String) mapZip.get("pwd"));
            }

            writeBackResult.setFlag(true);

            File fileTarget = FileUtil.file(path, FileUtil.getName(fileOut));
            FileUtil.move(fileOut, fileTarget, true);

            writeBackResult.setFile(fileTarget.getAbsolutePath());

        } else {
            writeBackResult.setFlag(false);
        }

        return writeBackResult;
    }

}
