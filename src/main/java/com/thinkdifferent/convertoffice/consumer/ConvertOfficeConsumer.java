package com.thinkdifferent.convertoffice.consumer;

import com.thinkdifferent.convertoffice.config.RabbitMQConfig;
import com.thinkdifferent.convertoffice.service.ConvertOfficeService;
import com.thinkdifferent.convertoffice.task.Task;
import net.sf.json.JSONObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConvertOfficeConsumer {

    @Autowired
    private Task task;
    @Autowired
    private ConvertOfficeService convertOfficeService;

    /**
     * 队列消费者-转换MP4文件。启动多线程任务，处理队列中的消息
     *
     * @param strData 队列中放入的JSON字符串
     */
    @RabbitListener(queues  = RabbitMQConfig.QUEUE_RECEIVE)
    public void receiveTodoRequestByMap(String strData){
        try{
            JSONObject jsonData = JSONObject.fromObject(strData);
            task.doTask(convertOfficeService, jsonData);
            //	      Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
