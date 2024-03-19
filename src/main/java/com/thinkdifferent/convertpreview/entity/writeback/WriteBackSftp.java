package com.thinkdifferent.convertpreview.entity.writeback;

import cn.hutool.core.map.MapUtil;
import cn.hutool.extra.ssh.Sftp;
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
 * @author zhangzhen
 * @version 1.0
 * @date 2023/7/4 10:18
 */
@Log4j2
@Data
public class WriteBackSftp extends WriteBack {
    /**
     * sftp服务的访问地址
     */
    private String host;
    /**
     * sftp服务的访问端口
     */
    private Integer port;
    /**
     * sftp服务的用户名
     */
    private String username;
    /**
     * sftp服务的密码
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
        if (StringUtils.startsWith(input, "sftp://")) {
            // 解析路径，获取host\端口\路径\文件名
            String subFtpFilePath = input.substring(6);
            // 用户、密码
            if (subFtpFilePath.contains("@")) {
                this.setUsername(subFtpFilePath.substring(0, subFtpFilePath.indexOf(":")));
                this.setPassword(subFtpFilePath.substring(subFtpFilePath.indexOf(":") + 1, subFtpFilePath.indexOf("@")));
                // 获取完用户密码后, 重新截取
                subFtpFilePath = subFtpFilePath.substring(subFtpFilePath.indexOf("@") + 1);
            } else {
                // 匿名登录
                this.setUsername("anonymous");
                this.setPassword("");
            }

            // 文件路径开始下标
            int index = subFtpFilePath.contains(":") ? subFtpFilePath.indexOf(":") : subFtpFilePath.indexOf("/");
            this.setHost(subFtpFilePath.substring(0, index));
            this.setPort(subFtpFilePath.contains(":") ? Integer.parseInt(subFtpFilePath.substring(index, subFtpFilePath.indexOf("/"))) : 22);
            this.setFilePath(subFtpFilePath.substring(subFtpFilePath.indexOf("/")));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public WriteBack of(Map<String, Object> writeBack) {
        WriteBackSftp writeBackSftp = new WriteBackSftp();
        String host = MapUtil.getStr(writeBack, "host");
        if (StringUtils.startsWith(host, "sftp://")) {
            host = host.substring(7);
        }
        writeBackSftp.setHost(host);
        writeBackSftp.setPort(MapUtil.getInt(writeBack, "port", 22));
        writeBackSftp.setUsername(MapUtil.getStr(writeBack, "username"));
        writeBackSftp.setPassword(MapUtil.getStr(writeBack, "password"));
        writeBackSftp.setFilePath(MapUtil.getStr(writeBack, "filepath"));
        return writeBackSftp;
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
        @Cleanup Sftp sftp = new Sftp(host, port, username, password);

        if(!sftp.exist(filePath)){
            sftp.mkDirs(filePath);
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

                if (!sftp.upload(filePath, new File(strJpg))) {
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
            if (!sftp.upload(filePath, fileOut)) {
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
