package com.thinkdifferent.convertpreview.service;

import net.sf.json.JSONObject;

public interface RabbitMQService {
    /**
     * 将传入的待处理文件的地址，记录到队列中，处理完毕后调用应用接口，回写文件和数据。
     * @param jsonInput 输入的JSON数据对象，包括文件地址、回写接口
     */
    void  setData2MQ(JSONObject jsonInput);

    /**
     * 发送重试MQ
     * 自动判断是否启用MQ
     * 自动判断是否超过最大重试次数， 超过最大重试队列后，在 /listError 接口可看到错误数据
     * @see com.thinkdifferent.convertpreview.controller.IndexController#listError()
     * @param jsonInput 重试消息
     */
    void setRetryData2MQ(JSONObject jsonInput);
}
