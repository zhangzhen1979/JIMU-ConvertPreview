package com.thinkdifferent.convertpreview.engine.impl.wpsServer;

import cn.hutool.core.io.FileUtil;
import net.sf.json.JSONException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * wps v6 下载重试
 *
 * @author ltian
 * @version 1.0
 * @date 2024/1/5 10:23
 */
@Component
@ConditionalOnProperty(name = "convert.engine.wpsPreview.enabled", havingValue = "true")
public class WpsServerV6ReTryUtil {

    // 指定ak/sk
    @Value("${convert.engine.wpsPreview.app_id:}")
    private String appId;
    @Value("${convert.engine.wpsPreview.app_key:}")
    private String appKey;
    @Value("${convert.engine.wpsPreview.domain:}")
    private String wpsDomain;

    public String checkDomain(String strDomain) {
        if (strDomain.endsWith("/")) {
            strDomain = strDomain.substring(0, strDomain.length() - 1);
        }
        if (strDomain.endsWith(":80")) {
            strDomain = strDomain.substring(0, strDomain.length() - 3);
        }
        return strDomain;
    }

    /**
     * 接口所在的域
     */
    public static final String OPEN_API = "/open";

    /**
     * 获取WPS转换结果
     *
     * @param taskId          任务ID
     * @param strDestFilePath 转换后文件地址
     * @return 转换后文件
     */
    @Retryable(
            // 捕获异常
            value = {RuntimeException.class, JSONException.class, IllegalArgumentException.class},
            // 最大重试次数
            maxAttempts = 5,
            // multiplier * delay = 下次重试时间
            backoff = @Backoff(multiplier = 2, delay = 30000)
    )
    public File downloadFile(String taskId, String strRouteKey, String strDestFilePath) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Route-Key", strRouteKey);

        String url = checkDomain(wpsDomain) + OPEN_API + "/api/cps/v1/download/" + taskId;

        cn.hutool.http.HttpResponse httpResponse = cn.hutool.http.HttpRequest.get(url)
                .addHeaders(buildHeaders(url, "", "GET", headers))
                .execute();
        if (httpResponse.isOk()) {
            InputStream fileInputStream = httpResponse.bodyStream();
            return FileUtil.writeFromStream(fileInputStream, strDestFilePath);
        }
        throw new Exception("下载文件异常, downloadId:" + taskId + ", routeKey:" + strRouteKey);
    }

    public Map<String, String> buildHeaders(String strURL, String strBody, String strMethod,
                                            Map<String, String> mapHeaders) throws Exception {
        //日期格式化
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = dateFormat.format(new Date());
        // 获取uri路径
        URL tUrl = new URL(strURL);
        String path = tUrl.getPath();

        //open不参与签名，做替换处理
        if (path.contains("/open")) {
            path = path.substring(path.indexOf("/open") + 5);
        }

        byte[] body = strBody.getBytes(StandardCharsets.UTF_8);
        String sha256body;
        //body为空则为空，否则返回sha256(body)
        if (body.length > 0) {
            sha256body = HMacUtils.getSHA256StrJava(body);
        } else {
            sha256body = "";
        }
        String signature = HMacUtils.HMACSHA256("WPS-4" + strMethod +
                path + "application/json" + date + sha256body, appKey);

        if (Objects.isNull(mapHeaders)) {
            mapHeaders = new HashMap<>();
        }

        mapHeaders.put("Content-Type", "application/json");
        mapHeaders.put("Wps-Docs-Date", date);
        mapHeaders.put("Wps-Docs-Authorization", String.format("WPS-4 %s:%s", appId, signature));

        return mapHeaders;
    }

}
