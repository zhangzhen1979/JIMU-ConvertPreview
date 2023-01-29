package com.thinkdifferent.convertpreview.entity.input;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * web地址
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 11:03
 */
@Log4j2
public class InputUrl extends Input {
    /**
     * 需转换的输入文件在Web服务中的URL地址
     */
    private String url;
    private String fileType;

    public boolean matchInput(String inputStr) {
        return StringUtils.startsWithAny(inputStr, "http://", "https://");
    }

    @Override
    public Input of(String inputPath, String strExt) {
        InputUrl path = new InputUrl();
        path.setUrl(inputPath);
        path.setFileType(strExt);
        return path;
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


    @Override
    public File getInputFile() {
        // 文件临时存储路径
        if (super.inputFile == null) {
            String strInputFileName = getFileNameFromHeader();
            // 移除重名文件
            String downloadFilePath = getBaseUrl() + strInputFileName;
            FileUtil.del(downloadFilePath);
            // 从指定的URL中将文件读取下载到目标路径
            log.debug("url:" + url);
            log.debug("downloadFilePath:" + downloadFilePath);
            HttpUtil.downloadFile(url, downloadFilePath);
            Assert.isTrue(FileUtil.exist(downloadFilePath), this.url + "下载文件失败");
            // log.info("下载【{}】文件【{}】成功", this.url, downloadFilePath);
            super.setInputFile(new File(downloadFilePath));
        }
        return super.inputFile;
    }

    @SneakyThrows
    private String getFileNameFromHeader() {
        String fileName = null;
        URL urlConnection = new URL(this.url);
        URLConnection uc = urlConnection.openConnection();

        String uuid = "";
        if (this.url.contains("?") && this.url.contains("uuid=")) {
            // 取UUID
            String subUuid = this.url.substring(this.url.indexOf("uuid="));
            uuid = subUuid.substring(5, subUuid.contains("&") ? subUuid.indexOf("&") : subUuid.length());
        }

        String realFileName = "";
        // 优先从header获取， 获取失败截取http请求最后内容
        String headerField = uc.getHeaderField("Content-Disposition");
        if (StringUtils.isNotBlank(headerField)) {
            fileName = new String(headerField.getBytes(StandardCharsets.ISO_8859_1), "GBK");
            realFileName = URLDecoder.decode(fileName.substring(fileName.indexOf("filename=") + 9), "UTF-8");
        } else {
            realFileName = UUID.randomUUID().toString() + "." + this.fileType;
        }
        return StringUtils.isNotBlank(uuid) ? (uuid + realFileName.substring(realFileName.lastIndexOf("."))) : realFileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }


}
