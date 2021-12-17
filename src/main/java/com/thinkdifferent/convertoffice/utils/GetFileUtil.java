package com.thinkdifferent.convertoffice.utils;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Map;

@Component
public class GetFileUtil {

    private static Logger logger = LoggerFactory.getLogger(GetFileUtil.class);

    /**
     * 从url中以get方式获取文件。可以传入Headers
     * @param strURL 文件访问URL
     * @param mapHeaders Headers参数
     * @return
     * @throws IOException
     */
    public static byte[] getFile(String strURL, Map<String, String> mapHeaders) throws IOException {
        logger.info("Ready Get Request Url[{}]", strURL);
        HttpGet httpGet = new HttpGet(strURL);
        httpGet.setHeaders(getHeaders(mapHeaders));
        HttpResponse httpResponse = HttpClients.createDefault().execute(httpGet);
        if (null == httpResponse || httpResponse.getStatusLine() == null) {
            logger.info("Post Request For Url[{}] is not ok. Response is null", strURL);
            return null;
        } else if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            logger.info("Post Request For Url[{}] is not ok. Response Status Code is {}", strURL,
                    httpResponse.getStatusLine().getStatusCode());
            return null;
        }
        return EntityUtils.toByteArray(httpResponse.getEntity());
    }

    /**
     * 将传入的Map参数转换为Header对象数组
     * @param mapHeaders Header参数
     * @return Header对象数组
     */
    public static Header[] getHeaders(Map<String, String> mapHeaders){
        Header[] headers = new Header[mapHeaders.size()];

        int i = 0;
        for (Map.Entry<String, String> entry : mapHeaders.entrySet()) {
            logger.info("Header -- key = " + entry.getKey() + ", value = " + entry.getValue());
            headers[i] = new BasicHeader(entry.getKey(), entry.getValue());
            i++;
        }

        return headers;
    }

    /**
     * 将byte数组写入到文件
     * @param byteArray 文件的byte数组
     * @param strTargetPath 目标文件夹和文件名
     * @return File文件对象
     */
    public static File byte2File(byte[] byteArray, String strTargetPath) {
        InputStream inputStream = new ByteArrayInputStream(byteArray);
        File file = new File(strTargetPath);
        String strPath = strTargetPath.substring(0, strTargetPath.lastIndexOf("/"));
        if (!file.exists()) {
            new File(strPath).mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            int intLen = 0;
            byte[] byteBuf = new byte[1024];
            while ((intLen = inputStream.read(byteBuf)) != -1) {
                fos.write(byteBuf, 0, intLen);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return file;
    }

}
