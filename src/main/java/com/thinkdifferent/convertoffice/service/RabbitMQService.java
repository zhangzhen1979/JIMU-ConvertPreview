package com.thinkdifferent.convertoffice.service;

import net.sf.json.JSONObject;

public interface RabbitMQService {
    /**
     * 将传入的待处理文件的地址，记录到队列中，处理完毕后调用应用接口，回写文件和数据。
     * @param jsonInput 输入的JSON数据对象，包括文件地址、回写接口
     */
    void  setData2MQ(JSONObject jsonInput);
}
