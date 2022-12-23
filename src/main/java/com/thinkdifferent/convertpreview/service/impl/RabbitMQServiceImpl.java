package com.thinkdifferent.convertpreview.service.impl;

import com.thinkdifferent.convertpreview.config.RabbitMQConfig;
import com.thinkdifferent.convertpreview.config.SystemConstants;
import com.thinkdifferent.convertpreview.service.RabbitMQService;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.UUID;

import static com.thinkdifferent.convertpreview.config.RabbitMQConfig.DELAY_EXCHANGE_RETRY;
import static com.thinkdifferent.convertpreview.config.RabbitMQConfig.DELAY_ROUTING_RECEIVE_RETRY;


@Service
@Log4j2
public class RabbitMQServiceImpl implements RabbitMQService, RabbitTemplate.ConfirmCallback {

    //由于rabbitTemplate的scope属性设置为ConfigurableBeanFactory.SCOPE_PROTOTYPE，所以不能自动注入
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /**
     * 重试次数, 默认不开启重试
     */
    @Value("${convert.retry.max:0}")
    private int maxRetryNum;

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
     *
     * @param jsonInput 输入的JSON数据对象
     */
    public void setData2MQ(JSONObject jsonInput) {
        // 获取开始时间
        long longStartTime = System.currentTimeMillis();

        try {
            // 将传入的数据JSON，放入到MQ服务器的receive队列中。
            CorrelationData correlationId = new CorrelationData(UUID.randomUUID().toString());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXECHANGE_RECEIVE, RabbitMQConfig.ROUTING_RECEIVE,
                    jsonInput.toString(), correlationId);

            long longEndTime = System.currentTimeMillis();    //获取结束时间

            log.info("数据JSON发送成功！ID:" + correlationId.getId() + "， 耗时：" + (longEndTime - longStartTime) + " ms");

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
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
        // 获取开始时间
        long longStartTime = System.currentTimeMillis();

        log.info(" 回调id:" + correlationData);
        if (b) {
            log.info("消息成功消费");
        } else {
            log.info("消息消费失败:" + msg);
        }


        long longEndTime = System.currentTimeMillis();    //获取结束时间

        log.info("回调ID(" + correlationData.getId() + ")！confirm 耗时：" + (longEndTime - longStartTime) + " ms");
    }

    /**
     * 重试时间配置
     */
    private static final LinkedList<Integer> RETRY_DELAY_TIME = new LinkedList() {{
        // 0
        add(0, 0);
        // 5min
        add(1, 1000 * 60 * 5);
        // 10min
        add(2, 1000 * 60 * 10);
        // 30min
        add(3, 1000 * 60 * 30);
        // 1h
        add(4, 1000 * 60 * 60);
        // 2h
        add(5, 1000 * 60 * 60 * 2);
        // 4h
        add(6, 1000 * 60 * 60 * 4);
        // 8h
        add(7, 1000 * 60 * 60 * 8);
        // 16h
        add(8, 1000 * 60 * 60 * 16);
    }};

    /**
     * 发送重试MQ
     * 自动判断是否启用MQ
     * 自动判断是否超过最大重试次数， 超过最大重试队列后，在 /listError 接口可看到错误数据
     *
     * @param jsonInput 重试消息
     * @see com.thinkdifferent.convertpreview.controller.IndexController#listError()
     */
    @Override
    public void setRetryData2MQ(JSONObject jsonInput) {
        if (!RabbitMQConfig.producer) {
            // 记录错误数据
            SystemConstants.addErrorData(jsonInput);
            return;
        }
        try {
            // 获取当前重试次数
            int currentRetryNum = jsonInput.containsKey(SystemConstants.RETRY_KEY) ? jsonInput.getInt(SystemConstants.RETRY_KEY) : 0;
            int nextRetryNum = ++currentRetryNum ;
            if (nextRetryNum > maxRetryNum) {
                log.error("已超过最大重试次数，请检查。data={}", jsonInput);
                // 记录错误数据
                SystemConstants.addErrorData(jsonInput);
                return;
            }

            log.info("开始第{}次重试，data={}", currentRetryNum, jsonInput);
            jsonInput.put(SystemConstants.RETRY_KEY, currentRetryNum);

            this.rabbitTemplate.convertAndSend(
                    DELAY_EXCHANGE_RETRY,
                    DELAY_ROUTING_RECEIVE_RETRY,
                    jsonInput.toString(),
                    message -> {
                        //配置消息的过期时间
                        message.getMessageProperties().setDelay(RETRY_DELAY_TIME.get(nextRetryNum));
                        return message;
                    }
            );
            log.info("第{}次重试数据JSON发送成功！", currentRetryNum);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("发送失败重试出现异常, data=" + jsonInput, e);
        }
    }

}
