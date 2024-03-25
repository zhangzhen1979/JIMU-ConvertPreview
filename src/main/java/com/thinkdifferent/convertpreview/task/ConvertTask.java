package com.thinkdifferent.convertpreview.task;

import com.thinkdifferent.convertpreview.config.SystemConstants;
import com.thinkdifferent.convertpreview.entity.*;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.service.RabbitMQService;
import com.thinkdifferent.convertpreview.utils.WriteBackUtil;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Map;

@Component
@Log4j2
public class ConvertTask implements RabbitTemplate.ConfirmCallback {
    @Resource
    private RabbitMQService rabbitMQService;
    @Resource
    private ConvertService convertService;

    /**
     * 处理接收列表中的数据，异步多线程任务
     *
     * @param jsonInput 队列中待处理的JSON数据
     * @throws Exception
     */
    @Async("taskExecutor")
    public void doTask(JSONObject jsonInput) {

        log.info("开始处理-异步转换文件");
        long longStart = System.currentTimeMillis();

        log.info("MQ中存储的数据:" + jsonInput.toString());

        CallBackResult callBackResult = new CallBackResult(false);
        try {
            TargetFile targetFile = convertService.convert(jsonInput);

            String strOutType = jsonInput.getString("outPutFileType");
            if (StringUtils.equalsAnyIgnoreCase(strOutType, "pdf", "ofd", "jpg")) {
                ConvertDocEntity convertDocEntity = ConvertDocEntity.of(jsonInput);
                if (convertDocEntity.getCallBackURL() != null) {
                    String strCallBackURL = convertDocEntity.getCallBackURL();
                    Map<String, String> mapWriteBackHeaders = convertDocEntity.getCallBackHeaders();
                    WriteBackResult writeBackResult = WriteBackUtil.writeBack(
                            convertDocEntity.getWriteBack(),
                            convertDocEntity.getOutPutFileType(),
                            targetFile,
                            null,
                            convertDocEntity.getZipParam());
                    String outPutFileName = convertDocEntity.getOutPutFileName();

                    callBackResult = convertService.callBack(strCallBackURL, mapWriteBackHeaders,
                            writeBackResult, outPutFileName);
                }
            } else {
                ConvertVideoEntity convertVideoEntity = ConvertVideoEntity.of(jsonInput);
                if (convertVideoEntity.getCallBackURL() != null) {
                    String strCallBackURL = convertVideoEntity.getCallBackURL();
                    Map<String, String> mapWriteBackHeaders = convertVideoEntity.getCallBackHeaders();
                    WriteBackResult writeBackResult = WriteBackUtil.writeBack(
                            convertVideoEntity.getWriteBack(),
                            "mp4",
                            targetFile,
                            null,
                            null);
                    ;
                    String outPutFileName = convertVideoEntity.getOutPutFileName();

                    callBackResult = convertService.callBack(strCallBackURL, mapWriteBackHeaders,
                            writeBackResult, outPutFileName);
                }

            }

        } catch (Exception | Error e) {
            e.printStackTrace();
            log.error("通过MQ异步转换异常", e);
            callBackResult.setMessage("Convert with MQ Error.");
        } finally {
            if (callBackResult.isFlag()) {
                // 成功，清理失败记录
                SystemConstants.removeErrorData(jsonInput);
            } else {
                // 异常情况重试
                rabbitMQService.setRetryData2MQ(jsonInput);
            }
        }
        long longEnd = System.currentTimeMillis();
        log.info("完成异步转换，耗时：" + (longEnd - longStart) + "毫秒");
    }

    /**
     * 回调反馈消费者消费信息
     *
     * @param correlationData
     * @param b
     * @param msg
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean b, String msg) {
        log.info(" 回调id:" + correlationData);
        if (b) {
            log.info("消息成功消费");
        } else {
            log.info("消息消费失败:" + msg);
        }
    }


}
