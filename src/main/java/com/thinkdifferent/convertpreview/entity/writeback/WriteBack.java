package com.thinkdifferent.convertpreview.entity.writeback;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.entity.ZipParam;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 回写父类
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 10:14
 */
public abstract class WriteBack {
    /**
     * 是否匹配
     * @param input 输入内容
     * @return 是否匹配
     */
    public abstract boolean match(String input);
    /**
     * 对象转换
     *
     * @param writeBack 传入的完整参数map
     * @return wb
     */
    public abstract WriteBack of(Map<String, Object> writeBack);

    /**
     * @return jpg文件输出路径
     */
    public String getOutputPath() {
        return ConvertDocConfigBase.outPutPath;
    }

    /**
     * 转换结果回写
     *
     * @param fileOut        转换后的文件
     */
    public WriteBackResult writeBack(File fileOut) throws ZipException {
        return writeBack(FileUtil.extName(fileOut), fileOut, null, null);
    }

    /**
    /**
     * 转换结果回写
     *
     * @param outPutFileType 目标文件类型
     * @param fileOut        转换后的文件
     * @param listJpg        转换后的jpg文件
     * @param zipParam       zip压缩参数
     */
    public abstract WriteBackResult writeBack(String outPutFileType, File fileOut, List<String> listJpg, ZipParam zipParam)
            throws ZipException;

    public Map<String,Object> getZip(File fileOut, ZipParam zipParam) throws ZipException {
        Map<String, Object> mapReturn = new HashMap<>();
        mapReturn.put("flag", false);
        if(zipParam != null){
            String strFileName = SystemUtil.beautifulFilePath(fileOut.getAbsolutePath());
            strFileName = strFileName.substring(strFileName.lastIndexOf("/") + 1);

            String strDest = SystemUtil.beautifulFilePath(fileOut.getAbsolutePath());
            if(zipParam.isHasExt()){
                strDest = strDest.substring(0, strDest.lastIndexOf(".")) + ".zip";
            }else{
                strDest = strDest.substring(0, strDest.lastIndexOf("."));
            }

            mapReturn = ZipParam.checkToZip(zipParam, strDest, fileOut, strFileName);
            mapReturn.put("flag", true);
        }

        return mapReturn;
    }


}
