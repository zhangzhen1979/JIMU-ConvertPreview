package com.thinkdifferent.convertpreview.engine.impl.fcs;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineYzFCS;
import com.thinkdifferent.convertpreview.engine.impl.AbstractEngineServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * 永中FCS服务引擎
 *
 * @author 张镇
 * @version 3.0
 * @date 2024/1/5 10:42
 */
@Service
@ConditionalOnProperty(name = "convert.engine.fcs.enabled", havingValue = "true")
@Log4j2
public class FCSEngineServiceImpl extends AbstractEngineServiceImpl {

    /**
     * FCS引擎实现的文件转换
     *
     * @param inputFile      输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @SneakyThrows
    @Override
    public File doConvertPdf(File inputFile, String strOutputFilePath){
        File fileReturn = new File(strOutputFilePath);

        FileBody fileBody = new FileBody(inputFile);

        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        multipartEntityBuilder.setCharset(Charset.forName("UTF-8"));
        multipartEntityBuilder.addPart("file", fileBody);
        multipartEntityBuilder.addPart("isDelSrc",
                new StringBody("1", ContentType.TEXT_PLAIN));
        multipartEntityBuilder.addPart("targetUrl",
                new StringBody("1", ContentType.TEXT_PLAIN));
        // 转换目标格式：pdf：3；jpg：30；html：0
        multipartEntityBuilder.addPart("convertType",
                new StringBody("3", ContentType.TEXT_PLAIN));
        // 是否显示修订记录
        multipartEntityBuilder.addPart("acceptTracks",
                new StringBody(ConvertDocConfigEngineYzFCS.fcsAcceptTracks.toString(), ContentType.TEXT_PLAIN));

        String strConvertURL = ConvertDocConfigEngineYzFCS.fcsDomain;

        CloseableHttpClient client = HttpClients.createDefault();;
        HttpPost httpPost = new HttpPost(strConvertURL);
        httpPost.setEntity(multipartEntityBuilder.build());
        HttpResponse response = client.execute(httpPost);

        log.debug(response.toString());

        if (response.getStatusLine().getStatusCode() == 200) {
            // 取回服务器端的响应结果
            HttpEntity resEntity = response.getEntity();

            byte[] json= EntityUtils.toByteArray(resEntity);
            String strJSON=new String(json,"UTF-8");
            EntityUtils.consume(resEntity);

            if(strJSON == null || strJSON.isEmpty()){
                fileReturn = null;
            }else{
                JSONObject jsonObject = JSONObject.fromObject(strJSON);
                int intCode = (Integer)jsonObject.get("errorcode");
                JSONObject joData = jsonObject.getJSONObject("data");
                if(intCode == 0 && joData != null){
                    String strFileURL = joData.getString("viewUrl");

                    cn.hutool.http.HttpResponse htResponse = HttpUtil.createGet(strFileURL).execute();

                    log.debug(htResponse.body());

                    FileUtil.writeBytes(htResponse.bodyBytes(), fileReturn);
                }
            }
        }

        client.close();

        return fileReturn;
    }

    /**
     * 该引擎支持的文件后缀
     *
     * @return list
     */
    @Override
    public List<String> supportFileExt() {
        return Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv");
    }
}
