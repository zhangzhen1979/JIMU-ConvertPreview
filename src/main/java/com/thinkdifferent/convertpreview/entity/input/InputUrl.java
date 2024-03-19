package com.thinkdifferent.convertpreview.entity.input;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import lombok.Data;
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
@Data
public class InputUrl extends Input {
    /**
     * 需转换的输入文件在Web服务中的URL地址
     */
    private String url;
    private String fileName;
    private String fileType;

    public boolean matchInput(String inputStr) {
        return StringUtils.startsWithAny(inputStr, "http://", "https://");
    }

    @Override
    public Input of(String inputPath, String strFileName, String strExt) {
        InputUrl path = new InputUrl();
        path.setUrl(inputPath);
        path.setFileName(strFileName);
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
        try {
            HttpResponse resp = HttpUtil.createGet(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                    .setMaxRedirectCount(10)
                    .execute();
            if (resp.isOk() && (resp.contentLength() > 0 || (Objects.nonNull(getInputFile()) && getInputFile().length() > 0))) {
                return true;
            }

            log.error("File not download. status:" + resp.getStatus() + "; fileLength: " + resp.contentLength() + ", URL: " + url);
            return false;
        } catch (Exception | Error e) {
            log.error("inputUrl 判断文件是否存在异常", e);
            return false;
        }

    }


    @Override
    public File getInputFile() {
        // 文件临时存储路径
        if (super.inputFile == null) {
            // 判断缓存中是否存在
            super.inputFile = super.getCacheFile(this.url);
            if (Objects.nonNull(super.inputFile)) {
                return super.inputFile;
            }

            if (fileName == null
                    || fileName.equalsIgnoreCase("null")
                    || fileName.isEmpty()) {
                String strInputFileName = getFileNameFromHeader();
                fileName = StringUtils.isNotBlank(strInputFileName) ? strInputFileName : (UUID.randomUUID() + "." + this.getFileType());
            }

            if (!StringUtils.isEmpty(fileName)) {
                // 判断文件是否存在
                String downloadFilePath = getInputTempPath() + fileName;
                File fileDownload = new File(downloadFilePath);
                if (fileDownload.exists()) {
                    if (fileDownload.length() == 0) {
                        // 如果文件大小为0，则删除，重新下载
                        FileUtil.del(fileDownload);
                    } else {
                        super.setInputFile(fileDownload);
                        super.addCache(this.url, downloadFilePath);
                        return super.inputFile;
                    }
                }
                // 从指定的URL中将文件读取下载到目标路径
                log.debug("url:" + url + " ;downloadFilePath:" + downloadFilePath);
                // 如果下载异常，则自动重试，每次间隔1秒
                for (int i = 0; i < ConvertDocConfigBase.maxRetryNum; i++) {
                    try {
                        HttpResponse execute = HttpUtil.createGet(url)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                                .setMaxRedirectCount(10)
                                .execute();

                        long longSize = execute.writeBody(downloadFilePath);
                        if (longSize > 0) {
                            break;
                        }
                        Thread.sleep(1000L);
                    } catch (Exception | Error e) {
                    }
                }
                Assert.isTrue(FileUtil.size(fileDownload) > 0, this.url + "下载文件失败");

                super.setInputFile(fileDownload);
                super.addCache(this.url, downloadFilePath);
            }
        }
        return super.inputFile;
    }

    @SneakyThrows
    private String getFileNameFromHeader() {
        if (!StringUtils.isEmpty(fileType)) {
            String fileName;
            URL urlConnection = new URL(this.url);
            URLConnection uc = urlConnection.openConnection();
            uc.setConnectTimeout(30 * 1000);

            String uuid = "";
            String md5 = "";
            if (this.url.contains("?")) {
                if (this.url.contains("md5=")) {
                    // 取md5作为文件名
                    md5 = this.url.substring(this.url.indexOf("md5="));
                    md5 = md5.substring(4, md5.contains("&") ? md5.indexOf("&") : md5.length());
                } else if (this.url.contains("uuid=")) {
                    // 取UUID
                    String subUuid = this.url.substring(this.url.indexOf("uuid="));
                    uuid = subUuid.substring(5, subUuid.contains("&") ? subUuid.indexOf("&") : subUuid.length());
                }
            }

            String realFileName = "";
            // 优先从header获取， 获取失败截取http请求最后内容
            String headerField = uc.getHeaderField("Content-Disposition");
            if (StringUtils.isNotBlank(headerField)) {
                fileName = new String(headerField.getBytes(StandardCharsets.ISO_8859_1), "GBK");
                realFileName = URLDecoder.decode(fileName.substring(fileName.indexOf("filename=") + 9), "UTF-8");
            } else {
                realFileName = UUID.randomUUID() + "." + this.fileType;
            }
            return StringUtils.isNotBlank(md5) ? (md5 + realFileName.substring(realFileName.lastIndexOf("."))) :
                    (StringUtils.isNotBlank(uuid) ? (uuid + realFileName.substring(realFileName.lastIndexOf("."))) : realFileName);

        }
        return null;
    }


}
