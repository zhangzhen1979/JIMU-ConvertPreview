package com.thinkdifferent.convertpreview.service;

import com.thinkdifferent.convertpreview.entity.CallBackResult;
import com.thinkdifferent.convertpreview.entity.input.Input;
import net.sf.json.JSONObject;
import org.springframework.scheduling.annotation.Async;

import javax.servlet.http.HttpServletResponse;
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
     * @param type       调用类型：convert，转换；base64，需要返回base64；stream，将文件信息返回Http响应头。
     * @param response   Http响应对象。
     */
    CallBackResult convert(Map<String, Object> parameters, String type, HttpServletResponse response);

    /**
     * 文件预览
     *
     * @param input   输入文件
     * @param params
     * @param outType 预览的类型 pdf 或 officePicture
     * @return 转换后的pdf文件
     */
    File filePreview(Input input, Map<String, String> params, String outType) throws Exception;
}
