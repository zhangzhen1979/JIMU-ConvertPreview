package com.thinkdifferent.convertpreview.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.thinkdifferent.convertpreview.config.ConvertVideoConfig;
import com.thinkdifferent.convertpreview.config.RabbitMQConfig;
import com.thinkdifferent.convertpreview.consts.Global;
import com.thinkdifferent.convertpreview.entity.*;
import com.thinkdifferent.convertpreview.entity.writeback.WriteBack;
import com.thinkdifferent.convertpreview.service.ConvertDocService;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.service.ConvertVideoService;
import com.thinkdifferent.convertpreview.service.RabbitMQService;
import com.thinkdifferent.convertpreview.utils.WriteBackUtil;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.util.Map;
import java.util.Objects;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/12/27 18:12
 */
@Log4j2
@Service
public class ConvertServiceImpl implements ConvertService {
    @Resource
    private RabbitMQService rabbitMQService;
    @Resource
    private ConvertDocService convertDocService;

    @Resource
    private ConvertVideoService convertVideoService;

    /**
     * 异步转换
     *
     * @param jsonInput 输入参数
     */
    @Async
    @Override
    public void asyncConvert(JSONObject jsonInput) {
        if (RabbitMQConfig.producer) {
            rabbitMQService.setData2MQ(jsonInput);
        } else {
            convert(jsonInput);
        }
    }

    /**
     * 同步转换，返回转换后的文件、多文件返回父级目录
     *
     * @param jsonInput 输入参数
     * @return 转换后文件
     */
    @Override
    public TargetFile convert(JSONObject jsonInput) {
        TargetFile targetFile = null;
        String outPutFileType = null;
        WriteBack writeBack = null;
        String callBackUrl = null;
        Map<String, String> callBackHeaders = null;
        ZipParam zipParam = null;
        String outPutFileName = jsonInput.getString("outPutFileName");

        try {
            if (isVideo(jsonInput)) {
                // 音视频格式处理
                jsonInput.put(Global.CONVERT_TYPE, ConvertTypeEnum.VIDEO);
                ConvertVideoEntity convertVideoEntity = ConvertVideoEntity.of(jsonInput);
                writeBack = convertVideoEntity.getWriteBack();
                callBackUrl = convertVideoEntity.getCallBackURL();
                callBackHeaders = convertVideoEntity.getCallBackHeaders();
                targetFile = convertVideoService.convert(convertVideoEntity, jsonInput.optString("$type"));
            } else {
                // 文档格式处理
                jsonInput.put(Global.CONVERT_TYPE, ConvertTypeEnum.IMG);
                ConvertDocEntity convertDocEntity = ConvertDocEntity.of(jsonInput);
                writeBack = Objects.requireNonNull(convertDocEntity).getWriteBack();
                callBackUrl = convertDocEntity.getCallBackURL();
                callBackHeaders = convertDocEntity.getCallBackHeaders();
                zipParam = convertDocEntity.getZipParam();
                targetFile = convertDocService.convert(convertDocEntity);
            }
            Assert.isTrue(StringUtils.equalsIgnoreCase(FileUtil.extName(targetFile.getTarget()), "m3u8")
                    || targetFile.getClass().getName().equals("java.io.File")
                            || FileUtil.exist(targetFile.getTarget()),
                    "转换失败");

            // 回写
            outPutFileType = FileUtil.extName(targetFile.getTarget());

            WriteBackResult writeBackResult = null;
            if (Objects.nonNull(targetFile)) {
                writeBackResult = WriteBackUtil.writeBack(
                        writeBack,
                        outPutFileType,
                        targetFile,
                        null,
                        zipParam);


                if (writeBackResult.getFile() != null) {
                    targetFile.setTarget(new File(writeBackResult.getFile()));
                }
            }

            writeBackResult.setPageNum(targetFile.getLongPageCount());

            callBack(callBackUrl, callBackHeaders, writeBackResult, outPutFileName);

            return targetFile;
        } catch (Exception | Error e) {
            log.error("转换失败", e);
            callBack(callBackUrl, callBackHeaders, null, outPutFileName);
        }

        return null;
    }

    /**
     * 是否是视频转换
     *
     * @param jsonInput 接收到的参数
     * @return bln
     */
    private boolean isVideo(JSONObject jsonInput) {
        if (jsonInput.containsKey("inputFiles")) {
            // doc 转换
            return false;
        }
        return ConvertVideoConfig.extList.contains(jsonInput.getString("inputFileType"));
    }

    /**
     * 回调业务系统提供的接口
     *
     * @param strCallBackURL      回调接口URL
     * @param mapWriteBackHeaders 请求头参数
     * @param writeBackResult     参数
     * @param outPutFileName      文件名
     * @return JSON格式的返回结果
     */
    @Override
    public CallBackResult callBack(String strCallBackURL, Map<String, String> mapWriteBackHeaders,
                                   WriteBackResult writeBackResult, String outPutFileName) {
        if (StringUtils.isBlank(strCallBackURL)) {
            log.info("文件{}回调地址为空，跳过", outPutFileName);
            return new CallBackResult(true, "回调地址为空，跳过");
        }

        // 回调路径预处理，截取?后的部分
        Map<String, Object> mapParams = writeBackResult.bean2Map();
        if (strCallBackURL.contains("?")) {
            String strInputParams = strCallBackURL.substring(strCallBackURL.indexOf("?") + 1);
            String[] strsParams = strInputParams.split("&");
            for (String strParam : strsParams) {
                String[] p = strParam.split("=");
                if (p.length == 2) {
                    mapParams.put(p[0], p[1]);
                }
            }

            strCallBackURL = strCallBackURL.substring(0, strCallBackURL.indexOf("?"));
        }

        // 处理参数中的【file】参数，只要文件名。
        if(mapParams.containsKey("file")){
            String strFile = (String)mapParams.get("file");
            mapParams.put("file", FileUtil.getName(strFile));
        }

        // 去除无用参数。
        if(mapParams.containsKey("tempFile")){
            mapParams.remove("tempFile");
        }

        //发送get请求并接收响应数据
        log.info("回调请求地址:{}, 请求体:{}", strCallBackURL, mapParams);
        try (HttpResponse httpResponse = HttpUtil.createGet(strCallBackURL)
                .addHeaders(mapWriteBackHeaders).form(mapParams)
                .execute()) {
            String body = httpResponse.body();
            log.info("状态码：{}，结果：{}", httpResponse.isOk(), body);

            if (httpResponse.isOk() && writeBackResult.isFlag()) {
                // 回调成功且转换成功，任务才会结束
                return new CallBackResult(true, "Convert Callback Success.\n" +
                        "Message is :\n" +
                        body);
            } else {
                return new CallBackResult(false, "CallBack error, resp: " + body + ", writeBackResult=" + writeBackResult);
            }
        }
    }
}
