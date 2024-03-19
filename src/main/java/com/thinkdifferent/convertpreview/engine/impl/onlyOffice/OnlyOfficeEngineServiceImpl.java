package com.thinkdifferent.convertpreview.engine.impl.onlyOffice;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineOnlyOffice;
import com.thinkdifferent.convertpreview.engine.impl.AbstractEngineServiceImpl;
import com.thinkdifferent.convertpreview.engine.impl.onlyOffice.vo.ConvertBody;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * OnlyOffice服务引擎
 *
 * @author 张镇
 * @version 3.0
 * @date 2024/1/5 10:42
 */
@Service
@ConditionalOnProperty(name = "convert.engine.onlyOffice.enabled", havingValue = "true")
@Log4j2
public class OnlyOfficeEngineServiceImpl extends AbstractEngineServiceImpl {

    /**
     * OnlyOffice引擎实现的文件转换
     *
     * @param inputFile         输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @SneakyThrows
    @Override
    public File doConvertPdf(File inputFile, String strOutputFilePath) {
        File fileReturn = new File(strOutputFilePath);

        String strInputFile = SystemUtil.beautifulFilePath(inputFile.getAbsolutePath());
        String strFileName = strInputFile.substring(strInputFile.lastIndexOf("/") + 1);
        String strExt = strInputFile.substring(strInputFile.lastIndexOf(".") + 1);

        log.debug("使用OnlyOffice Server，开始转换文件。{}->{}", strInputFile, strOutputFilePath);

        // 原始文件下载地址
        String strFileUrl = SystemUtil.beautifulPath(ConvertDocConfigBase.baseUrl) + "infile/" + strFileName;

        ConvertBody convertBody = new ConvertBody(
                strExt,
                DocumentKey.SnowflakeId(),
                "pdf",
                strFileUrl,
                strFileName,
                "");
        String strBody = JSONObject.fromObject(convertBody).toString();

        if (null != ConvertDocConfigEngineOnlyOffice.onlyOfficeSecret) {
            String token = JWTUtil.createToken(JSONObject.fromObject(strBody), ConvertDocConfigEngineOnlyOffice.onlyOfficeSecret);
            convertBody.setToken(token);
            strBody = JSONObject.fromObject(convertBody).toString();
        }

        log.debug(strBody);

        byte[] bodyByte = strBody.getBytes(StandardCharsets.UTF_8);
        try {
            URL url = new URL(SystemUtil.beautifulPath(ConvertDocConfigEngineOnlyOffice.onlyOfficeDomain) + "ConvertService.ashx");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setFixedLengthStreamingMode(bodyByte.length);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(ConvertDocConfigEngineOnlyOffice.onlyOfficeTimeout);

            connection.connect();

            OutputStream os = connection.getOutputStream();
            os.write(bodyByte);

            InputStream stream = connection.getInputStream();

            if (stream == null) {
                throw new Exception("Could not get an answer");
            }

            /**将流转为字符串*/
            String jsonString = FileUtil.ConvertStreamToString(stream);

            connection.disconnect();

            JSONObject jsonObj = JSONObject.fromObject(jsonString);
            log.debug(jsonObj.toString());
            /**
             * {
             *     "endConvert":true，//转换是否完成
             *     "fileUrl"：“ https：//documentserver/ResourceService.ashx?filename=output.doc”，//转换后的文件地址
             *     "percent"：100//转换完成百分比 仅参数设置为异步时
             *  }
             */
            Object error = jsonObj.get("error");
            if (error != null) {
                ProcessConvertServiceResponceError((Integer) error);
            }

            /**检查转换是否完成，并将结果保存到一个变量中*/
            Boolean isEndConvert = (Boolean) jsonObj.get("endConvert");
            Long resultPercent = 0L;
            String responseUri = null;

            if (isEndConvert) {
                resultPercent = 100L;
                responseUri = (String) jsonObj.get("fileUrl");
                log.debug("使用OnlyOffice Server，转换文件完成。{}->{}", strInputFile, strOutputFilePath);
            } else {
                resultPercent = (Long) jsonObj.get("percent");
                resultPercent = resultPercent >= 100l ? 99l : resultPercent;
            }

            if (resultPercent >= 100L) {
                HttpResponse htResponse = HttpUtil.createGet(responseUri).execute();
                cn.hutool.core.io.FileUtil.writeBytes(htResponse.bodyBytes(), fileReturn);
            }


        } catch (Exception | Error e) {
            e.printStackTrace();
        }

        return fileReturn;
    }

    /**
     * 错误代码	描述
     * -1	未知错误。
     * -2	转换超时错误。
     * -3	转换错误。
     * -4	下载要转换的文档文件时出错。
     * -5	密码不正确。
     * -6	访问转换结果数据库时出错。
     * -7	输入错误。
     * -8	令牌无效。
     */
    private void ProcessConvertServiceResponceError(int errorCode) throws Exception {
        String errorMessage = "";
        String errorMessageTemplate = "Error occurred in the ConvertService: ";
        switch (errorCode) {
            case -8:
                errorMessage = errorMessageTemplate + "令牌无效";
                break;
            case -7:
                errorMessage = errorMessageTemplate + "输入错误";
                break;
            case -6:
                errorMessage = errorMessageTemplate + "访问转换结果数据库时出错";
                break;
            case -5:
                errorMessage = errorMessageTemplate + "密码不正确";
                break;
            case -4:
                errorMessage = errorMessageTemplate + "下载要转换的文档文件时出错";
                break;
            case -3:
                errorMessage = errorMessageTemplate + "转换错误";
                break;
            case -2:
                errorMessage = errorMessageTemplate + "转换超时错误";
                break;
            case -1:
                errorMessage = errorMessageTemplate + "未知错误";
                break;
            case 0:
                break;
            default:
                errorMessage = "ErrorCode = " + errorCode;
                break;
        }
        throw new Exception(errorMessage);
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
