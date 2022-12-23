package com.thinkdifferent.convertpreview.utils;

import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpMode;
import com.thinkdifferent.convertpreview.entity.input.InputFtp;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

import java.io.File;

/**
 * ftp工具类
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/21 14:57
 */
@Log4j2
public class FtpUtil {

    /**
     * 默认的FTP模式
     */
    private static final FtpMode DEFAULT_FTP_MODE = FtpMode.Passive;

    private FtpUtil() {
    }

    /**
     * 下载FTP文件
     *
     * @param inputFtp     ftp配置
     * @param downloadFile 保存的文件
     */
    public static void downloadFile(InputFtp inputFtp, File downloadFile) {
        // 服务器不需要代理访问
        Ftp ftp = new Ftp(inputFtp.getHost(), inputFtp.getPort(), inputFtp.getUsername(), inputFtp.getPassword(),
                null, null, null, DEFAULT_FTP_MODE);
        // 切换目录
        ftp.cd(inputFtp.getFilePath());
        ftp.download(inputFtp.getFilePath(), downloadFile);
        IOUtils.closeQuietly(ftp);
    }


    /**
     * 判断FTP下文件是否存在
     *
     * @param inputFtp ftp对象
     * @return bln
     */
    public static boolean exist(InputFtp inputFtp){
        Ftp ftp = new Ftp(inputFtp.getHost(), inputFtp.getPort(), inputFtp.getUsername(), inputFtp.getPassword(),
                null, null, null, DEFAULT_FTP_MODE);
        // 切换目录
        return ftp.exist(inputFtp.getFilePath());
    }
}
