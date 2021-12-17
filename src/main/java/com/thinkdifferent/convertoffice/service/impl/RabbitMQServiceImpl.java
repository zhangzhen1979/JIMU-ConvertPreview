package com.thinkdifferent.convertoffice.service.impl;

import com.thinkdifferent.convertoffice.config.RabbitMQConfig;
import com.thinkdifferent.convertoffice.service.RabbitMQService;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RabbitMQServiceImpl implements RabbitMQService, RabbitTemplate.ConfirmCallback{

    private static final Logger log = LoggerFactory.getLogger(RabbitMQServiceImpl.class);

    //由于rabbitTemplate的scope属性设置为ConfigurableBeanFactory.SCOPE_PROTOTYPE，所以不能自动注入
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /**
     * 构造方法注入rabbitTemplate
     */
    @Autowired
    public RabbitMQServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setConfirmCallback(this); //rabbitTemplate如果为单例的话，那回调就是最后设置的内容
    }


    /**
     * 将传入的JSON对象，转换为PDF文件，输出到指定的目录中。
     * @param jsonInput 输入的JSON数据对象
     */
    public void  setData2MQ(JSONObject jsonInput) {
        // 获取开始时间
        long longStartTime = System.currentTimeMillis();

        try{
            // 将传入的数据JSON，放入到MQ服务器的receive队列中。
            CorrelationData correlationId = new CorrelationData(UUID.randomUUID().toString());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXECHANGE_RECEIVE, RabbitMQConfig.ROUTING_RECEIVE,
                    jsonInput.toString(), correlationId);

            long longEndTime = System.currentTimeMillis();    //获取结束时间

            log.info("数据JSON发送成功！ID:"+correlationId.getId()+"， 耗时："+(longEndTime - longStartTime)+" ms");

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }


    /**
     * 回调反馈消费者消费信息
     * @param correlationData
     * @param b
     * @param msg
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean b, String msg)
    {
        // 获取开始时间
        long longStartTime = System.currentTimeMillis();

        log.info(" 回调id:" + correlationData);
        if (b) {
            log.info("消息成功消费");
        } else {
            log.info("消息消费失败:" + msg);
        }


        long longEndTime = System.currentTimeMillis();    //获取结束时间

        log.info("回调ID("+correlationData.getId()+")！confirm 耗时："+(longEndTime - longStartTime)+" ms");
    }

}
