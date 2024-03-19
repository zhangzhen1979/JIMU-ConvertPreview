package com.thinkdifferent.convertpreview.entity.writeback;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpConfig;
import cn.hutool.extra.ftp.FtpMode;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.entity.ZipParam;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.Cleanup;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 10:18
 */
@Log4j2
@Data
public class WriteBackFtp extends WriteBack {
    /**
     * 是否是被动模式
     */
    private boolean passive;
    /**
     * ftp服务的访问地址
     */
    private String host;
    /**
     * ftp服务的访问端口
     */
    private Integer port;
    /**
     * ftp服务的用户名
     */
    private String username;
    /**
     * ftp服务的密码
     */
    private String password;
    /**
     * 文件所在路径
     */
    private String filePath;

    /**
     * 是否匹配
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        if (StringUtils.startsWith(input, "ftp://")) {
            // 解析路径，获取host\端口\路径\文件名
            String subFtpFilePath = input.substring(6);
            // 用户、密码
            if (subFtpFilePath.contains("@")) {
                this.setUsername(subFtpFilePath.substring(0, subFtpFilePath.indexOf(":")));
                this.setPassword(subFtpFilePath.substring(subFtpFilePath.indexOf(":") + 1, subFtpFilePath.lastIndexOf("@")));
                // 获取完用户密码后, 重新截取
                subFtpFilePath = subFtpFilePath.substring(subFtpFilePath.lastIndexOf("@") + 1);
            } else {
                // 匿名登录
                this.setUsername("anonymous");
                this.setPassword("");
            }

            // 文件路径开始下标
            int index = subFtpFilePath.contains(":") ? subFtpFilePath.indexOf(":") : subFtpFilePath.indexOf("/");
            this.setHost(subFtpFilePath.substring(0, index));
            this.setPort(subFtpFilePath.contains(":") ? Integer.parseInt(subFtpFilePath.substring(index, subFtpFilePath.indexOf("/"))) : 21);
            this.setFilePath(subFtpFilePath.substring(subFtpFilePath.indexOf("/")));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public WriteBack of(Map<String, Object> writeBack) {
        WriteBackFtp writeBackFtp = new WriteBackFtp();
        writeBackFtp.setPassive(MapUtil.getBool(writeBack, "passive", false));
        String host = MapUtil.getStr(writeBack, "host");
        if (StringUtils.startsWith(host, "ftp://")) {
            host = host.substring(6);
        }
        writeBackFtp.setHost(host);
        writeBackFtp.setPort(MapUtil.getInt(writeBack, "port", 21));
        writeBackFtp.setUsername(MapUtil.getStr(writeBack, "username"));
        writeBackFtp.setPassword(MapUtil.getStr(writeBack, "password"));
        writeBackFtp.setFilePath(MapUtil.getStr(writeBack, "filepath"));
        return writeBackFtp;
    }

    /**
     * 转换结果回写
     *
     * @param outPutFileType 目标文件类型
     * @param fileOut        转换后的文件
     * @param listJpg        转换后的jpg文件
     * @param zipParam       zip压缩参数
     */
    @SneakyThrows
    @Override
    public WriteBackResult writeBack(String outPutFileType, File fileOut, List<String> listJpg, ZipParam zipParam){
        WriteBackResult writeBackResult = new WriteBackResult();
        String strZipPWDs = "";

        // 回写是否成功
        boolean blnFlag = true;
        @Cleanup Ftp ftp = passive ?
                // 服务器需要代理访问，才能对外访问
                new Ftp(new FtpConfig(host, port, username, password, CharsetUtil.CHARSET_UTF_8), FtpMode.Passive) :
                // 服务器不需要代理访问
                new Ftp(host, port, username, password);
        if(!ftp.exist(filePath)){
            ftp.mkDirs(filePath);
        }

        if ("jpg".equalsIgnoreCase(outPutFileType)) {
            for (String strJpg : listJpg) {
                fileOut = new File(strJpg);
                Map<String,Object> mapZip = getZip(fileOut, zipParam);
                // 判断是否需要进行zip压缩
                if(mapZip != null && mapZip.get("flag").equals(true)){
                    fileOut = new File((String)mapZip.get("fileDest"));
                    strZipPWDs = strZipPWDs + mapZip.get("pwd") + ",";
                }

                if (!ftp.upload(filePath, fileOut)) {
                    blnFlag = false;
                }
            }
        } else {
            Map<String,Object> mapZip = getZip(fileOut, zipParam);
            // 判断是否需要进行zip压缩
            if(mapZip != null && mapZip.get("flag").equals(true)){
                fileOut = new File((String)mapZip.get("fileDest"));
                strZipPWDs = (String)mapZip.get("pwd");
            }

            // pdf 和 ofd 的都走这里
            if (!ftp.upload(filePath, fileOut)) {
                blnFlag = false;
            }
        }

        writeBackResult.setFlag(blnFlag);

        String strFileName = SystemUtil.beautifulFilePath(fileOut.getAbsolutePath());
        strFileName = strFileName.substring(strFileName.lastIndexOf("/")+1);
        writeBackResult.setFile(filePath + strFileName);
        writeBackResult.setZippwd(strZipPWDs);

        // 返回回写状态
        return writeBackResult;
    }

}
