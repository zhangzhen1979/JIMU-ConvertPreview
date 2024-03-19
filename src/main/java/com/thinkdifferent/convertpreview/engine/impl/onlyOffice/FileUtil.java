package com.thinkdifferent.convertpreview.engine.impl.onlyOffice;


import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;


/**
 * @BelongsProject: onlyoffice-demo
 * @BelongsPackage: com.onlyoffice.onlyoffice.tools
 * @Author: TongHui
 * @CreateTime: 2023-07-30 17:26
 * @Description: 文件操作
 * @Version: 1.0
 */
@Slf4j
public class FileUtil {

    private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    static String COMMA = ".";

    /**
     * 获取文件后缀 不带.
     *
     * @param name
     * @return
     */
    public static String getFileExtension(String name) {
        return FileNameUtil.extName(name);
    }

    /**
     * 读取远程文件
     *
     * @param url
     * @return
     */
    public static byte[] getFileByte(String url) {
        try (InputStream in = new URL(url).openConnection().getInputStream()) {
            return IoUtil.readBytes(in, 1024);
        } catch (IOException e) {
            log.error("读取失败:" + e.getMessage());
        }
        return new byte[0];
    }

    /**
     * 流 转字符串
     *
     * @param stream
     * @return
     * @throws IOException
     */
    public static String ConvertStreamToString(InputStream stream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();

        while (line != null) {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }

        String result = stringBuilder.toString();

        return result;
    }

    public static InputStream openInputStream(File file) {
        return IoUtil.toStream(file);
    }

    // 一般oo服务回调下载
    public static void downloadInline(File file, String name, HttpServletResponse response) {
        try {
            byte[] buffer = null;
            // 将文件写入输入流
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStream fis = new BufferedInputStream(fileInputStream);
            buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            dowInline(buffer, name, response);
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }

    public static void downloadInline(String path, String name, HttpServletResponse response) {
        logger.info("文件下载:" + path);
        byte[] buffer = null;
        try {
            if (path.startsWith("http")) {
                buffer = getFileByte(path);
                dowInline(buffer, name, response);
            } else {
                // path是指想要下载的文件的路径
                File file = new File(path);

                downloadInline(file, name, response);
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }

    }

    public static void downloadInline(byte[] bytes, String name, HttpServletResponse response) {
        logger.info("文件下载:" + name);
        dowInline(bytes, name, response);
    }


    //以附件形式下载 浏览器下载使用
    public static void downloadAttachment(File file, String name, HttpServletResponse response) {
        try {
            byte[] buffer = null;
            // 将文件写入输入流
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStream fis = new BufferedInputStream(fileInputStream);
            buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            attachment(buffer, name, response);
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }

    public static void downloadAttachment(String path, String name, HttpServletResponse response) {
        logger.info("文件下载:" + path);
        byte[] buffer = null;
        try {
            if (path.startsWith("http")) {
                buffer = getFileByte(path);
                downloadAttachment(buffer, name, response);
            } else {
                // path是指想要下载的文件的路径
                File file = new File(path);

                downloadAttachment(file, name, response);
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }

    public static void downloadAttachment(byte[] bytes, String name, HttpServletResponse response) {
        attachment(bytes, name, response);
    }

    private static void attachment(byte[] bytes, String name, HttpServletResponse response) {
        try {
            // 清空response
            response.reset();
            // 设置response的Header
            response.setCharacterEncoding("UTF-8");
            response.setContentType(getContentType(name));
            //Content-Disposition的作用：告知浏览器以何种方式显示响应返回的文件，用浏览器打开还是以附件的形式下载到本地保存
            //attachment表示以附件方式下载 inline表示在线打开 "Content-Disposition: inline; filename=文件名.mp3"
            // filename表示文件的默认名称，因为网络传输只支持URL编码的相关支付，因此需要将文件名URL编码后进行传输,前端收到后需要反编码才能获取到真正的名称
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name, "UTF-8"));
            // 告知浏览器文件的大小
            response.addHeader("Content-Length", "" + bytes.length);
            OutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void dowInline(byte[] bytes, String name, HttpServletResponse response) {
        try {
            // 清空response
            response.reset();
            // 设置response的Header
            response.setCharacterEncoding("UTF-8");
            response.setContentType(getContentType(name));
            //Content-Disposition的作用：告知浏览器以何种方式显示响应返回的文件，用浏览器打开还是以附件的形式下载到本地保存
            //attachment表示以附件方式下载 inline表示在线打开 "Content-Disposition: inline; filename=文件名.mp3"
            // filename表示文件的默认名称，因为网络传输只支持URL编码的相关支付，因此需要将文件名URL编码后进行传输,前端收到后需要反编码才能获取到真正的名称
            response.addHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(name, "UTF-8"));
            // 告知浏览器文件的大小
            response.addHeader("Content-Length", "" + bytes.length);
            OutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 根据“文件名的后缀”获取文件内容类型（而非根据File.getContentType()读取的文件类型）
     *
     * @param returnFileName 带验证的文件名
     * @return 返回文件类型
     */
    public static String getContentType(String returnFileName) {
        String contentType = "application/octet-stream";
        if (returnFileName.lastIndexOf(COMMA) < 0) {
            return contentType;
        }
        returnFileName = returnFileName.toLowerCase();
        returnFileName = returnFileName.substring(returnFileName.lastIndexOf(".") + 1);
        switch (returnFileName) {
            case "html":
            case "htm":
            case "shtml":
                contentType = "text/html";
                break;
            case "apk":
                contentType = "application/vnd.android.package-archive";
                break;
            case "sis":
                contentType = "application/vnd.symbian.install";
                break;
            case "sisx":
                contentType = "application/vnd.symbian.install";
                break;
            case "exe":
                contentType = "application/x-msdownload";
                break;
            case "msi":
                contentType = "application/x-msdownload";
                break;
            case "css":
                contentType = "text/css";
                break;
            case "xml":
                contentType = "text/xml";
                break;
            case "gif":
                contentType = "image/gif";
                break;
            case "jpeg":
            case "jpg":
                contentType = "image/jpeg";
                break;
            case "js":
                contentType = "application/x-javascript";
                break;
            case "atom":
                contentType = "application/atom+xml";
                break;
            case "rss":
                contentType = "application/rss+xml";
                break;
            case "mml":
                contentType = "text/mathml";
                break;
            case "txt":
                contentType = "text/plain";
                break;
            case "jad":
                contentType = "text/vnd.sun.j2me.app-descriptor";
                break;
            case "wml":
                contentType = "text/vnd.wap.wml";
                break;
            case "htc":
                contentType = "text/x-component";
                break;
            case "png":
                contentType = "image/png";
                break;
            case "tif":
            case "tiff":
                contentType = "image/tiff";
                break;
            case "wbmp":
                contentType = "image/vnd.wap.wbmp";
                break;
            case "ico":
                contentType = "image/x-icon";
                break;
            case "jng":
                contentType = "image/x-jng";
                break;
            case "bmp":
                contentType = "image/x-ms-bmp";
                break;
            case "svg":
                contentType = "image/svg+xml";
                break;
            case "jar":
            case "var":
            case "ear":
                contentType = "application/java-archive";
                break;
            case "doc":
                contentType = "application/msword";
                break;
            case "pdf":
                contentType = "application/pdf";
                break;
            case "rtf":
                contentType = "application/rtf";
                break;
            case "xls":
                contentType = "application/vnd.ms-excel";
                break;
            case "ppt":
                contentType = "application/vnd.ms-powerpoint";
                break;
            case "7z":
                contentType = "application/x-7z-compressed";
                break;
            case "rar":
                contentType = "application/x-rar-compressed";
                break;
            case "swf":
                contentType = "application/x-shockwave-flash";
                break;
            case "rpm":
                contentType = "application/x-redhat-package-manager";
                break;
            case "der":
            case "pem":
            case "crt":
                contentType = "application/x-x509-ca-cert";
                break;
            case "xhtml":
                contentType = "application/xhtml+xml";
                break;
            case "zip":
                contentType = "application/zip";
                break;
            case "mid":
            case "midi":
            case "kar":
                contentType = "audio/midi";
                break;
            case "mp3":
                contentType = "audio/mpeg";
                break;
            case "ogg":
                contentType = "audio/ogg";
                break;
            case "m4a":
                contentType = "audio/x-m4a";
                break;
            case "ra":
                contentType = "audio/x-realaudio";
                break;
            case "3gpp":
            case "3gp":
                contentType = "video/3gpp";
                break;
            case "mp4":
                contentType = "video/mp4";
                break;
            case "mpeg":
            case "mpg":
                contentType = "video/mpeg";
                break;
            case "mov":
                contentType = "video/quicktime";
                break;
            case "flv":
                contentType = "video/x-flv";
                break;
            case "m4v":
                contentType = "video/x-m4v";
                break;
            case "mng":
                contentType = "video/x-mng";
                break;
            case "asx":
            case "asf":
                contentType = "video/x-ms-asf";
                break;
            case "wmv":
                contentType = "video/x-ms-wmv";
                break;
            case "avi":
                contentType = "video/x-msvideo";
                break;
            default:
        }
        return contentType;
    }
}
