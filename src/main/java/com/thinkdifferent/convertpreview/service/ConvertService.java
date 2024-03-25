package com.thinkdifferent.convertpreview.service;

import com.thinkdifferent.convertpreview.entity.CallBackResult;
import com.thinkdifferent.convertpreview.entity.TargetFile;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import net.sf.json.JSONObject;

import java.util.Map;

/**
 * 转换接口，内部区分文档、音视频，统一输入输出
 *
 * @author ltian
 * @version 1.0
 * @date 2023/12/27 15:06
 */
public interface ConvertService {
    /**
     * 异步转换
     *
     * @param jsonInput 输入参数
     */
    void asyncConvert(JSONObject jsonInput);

    /**
     * 同步转换，返回转换后的文件、多文件返回父级目录
     *
     * @param jsonInput 输入参数
     * @return 转换后文件
     */
    TargetFile convert(JSONObject jsonInput);

    /**
     * 回调业务系统提供的接口
     *
     * @param strCallBackURL      回调接口URL
     * @param mapWriteBackHeaders 请求头参数
     * @param writeBackResult     参数
     * @param outPutFileName      文件名
     * @return JSON格式的返回结果
     */
    CallBackResult callBack(String strCallBackURL, Map<String, String> mapWriteBackHeaders,
                                   WriteBackResult writeBackResult, String outPutFileName);

}
