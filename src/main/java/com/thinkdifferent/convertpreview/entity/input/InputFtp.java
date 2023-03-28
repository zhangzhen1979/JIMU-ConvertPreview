package com.thinkdifferent.convertpreview.entity.input;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.utils.FtpUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 11:10
 */
public class InputFtp extends Input {
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
     * 检测输入字符串是否满足该输入类型
     *
     * @param inputStr 输入字符串
     * @return bln
     */
    @Override
    public boolean matchInput(String inputStr) {
        return StringUtils.startsWith(inputStr, "ftp://");
    }

    @Override
    public Input of(String inputPath, String strExt) {
        Assert.hasText(inputPath, "路径不能为空");
        Assert.isTrue(inputPath.startsWith("ftp://"), "FTP文件格式错误");
        InputFtp inputFtp = new InputFtp();
        // 解析路径，获取host\端口\路径\文件名  ftp://OA:Abc@456@192.168.1.1/
        String subFtpFilePath = inputPath.substring(6);
        // 用户、密码
        if (subFtpFilePath.contains("@")) {
            inputFtp.setUsername(subFtpFilePath.substring(0, subFtpFilePath.indexOf(":")));
            inputFtp.setPassword(subFtpFilePath.substring(subFtpFilePath.indexOf(":") + 1, subFtpFilePath.lastIndexOf("@")));
            // 获取完用户密码后, 重新截取
            subFtpFilePath = subFtpFilePath.substring(subFtpFilePath.lastIndexOf("@") + 1);
        } else {
            // 匿名登录
            inputFtp.setUsername("anonymous");
            inputFtp.setPassword("");
        }

        // 文件路径开始下标
        int index = subFtpFilePath.contains(":") ? subFtpFilePath.indexOf(":") : subFtpFilePath.indexOf("/");
        inputFtp.setHost(subFtpFilePath.substring(0, index));
        inputFtp.setPort(subFtpFilePath.contains(":") ? Integer.parseInt(subFtpFilePath.substring(index + 1, subFtpFilePath.indexOf("/"))) : 21);
        inputFtp.setFilePath(subFtpFilePath.substring(subFtpFilePath.indexOf("/")));
        return inputFtp;
    }

    /**
     * 判断传入资源是否存在
     *
     * @return bln
     */
    @Override
    public boolean exists() {
        return FtpUtil.exist(this);
    }

    @Override
    public File getInputFile() {
        if (super.inputFile == null) {
            // 判断缓存中是否存在
            super.inputFile = super.getCacheFile(this.filePath);
            if (super.inputFile == null) {
                // 判断文件是否存在
                String downloadFilePath = filePath.substring(filePath.lastIndexOf("/") + 1);
                File fileDownload = new File(downloadFilePath);
                if (fileDownload != null &&
                        fileDownload.exists() &&
                        fileDownload.length() > 0) {
                    super.setInputFile(fileDownload);
                    return super.inputFile;
                }

                // 如果文件大小为0，则删除，重新下载
                FileUtil.del(getBaseUrl() + downloadFilePath);
                // ftp 下载文件
                FtpUtil.downloadFile(this, new File(getBaseUrl() + downloadFilePath));
                super.setInputFile(new File(getBaseUrl() + downloadFilePath));
                super.addCache(this.filePath, getBaseUrl() + downloadFilePath);
            }

        }
        return super.inputFile;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
