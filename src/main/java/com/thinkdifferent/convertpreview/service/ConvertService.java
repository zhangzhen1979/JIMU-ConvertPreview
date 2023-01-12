package com.thinkdifferent.convertpreview.service;

import com.thinkdifferent.convertpreview.entity.CallBackResult;
import com.thinkdifferent.convertpreview.entity.input.Input;
import net.sf.json.JSONObject;
import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.util.Map;

public interface ConvertService {
    void checkParams(JSONObject jsonInput);

    @Async
    void asyncConvert(Map<String, Object> parameters);

    /**
     * 将传入的JSON对象中记录的文件，转换为MP4，输出到指定的目录中；回调应用系统接口，将数据写回。
     *
     * @param parameters 输入的参数，JSON格式数据对象
     * @param type       调用类型：convert，转换；base64，需要返回base64。
     */
    CallBackResult convert(Map<String, Object> parameters, String type);

    /**
     * 文件预览
     *
     * @param input   输入文件
     * @param params
     * @return 转换后的pdf文件
     */
    File filePreview(Input input, Map<String, String> params) throws Exception;
}
