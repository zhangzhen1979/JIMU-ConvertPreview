package com.thinkdifferent.convertpreview.consumer;

import com.thinkdifferent.convertpreview.config.RabbitMQConfig;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.task.Task;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Log4j2
public class ConvertConsumer {

    @Autowired
    private Task task;
    @Autowired
    private ConvertService convertService;

    /**
     * 队列消费者-转换MP4文件。启动多线程任务，处理队列中的消息
     *
     * @param strData 队列中放入的JSON字符串
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_RECEIVE)
    public void receiveTodoRequestByMap(String strData) {
        try {
            if (RabbitMQConfig.consumer) {
                JSONObject jsonData = JSONObject.fromObject(strData);
                task.doTask(convertService, jsonData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重试队列
     * @param message 重试消息
     */
    @RabbitListener(queues = RabbitMQConfig.DELAY_QUEUE_RETRY)
    public void consumeRetryMessage(Message message) {
        try {
            log.info("延时队列接收到信息：{}", message.getBody());
            String strData = new String(message.getBody());
            if (RabbitMQConfig.consumer) {
                JSONObject jsonData = JSONObject.fromObject(strData);
                task.doTask(convertService, jsonData);
            }
        } catch (Exception e) {
            log.error("重试出错，已重新重试", e);
        }
    }

}