package com.thinkdifferent.convertoffice.service;

import net.sf.json.JSONObject;

import java.util.Map;

public interface ConvertOfficeService {
    /**
     * 将传入的JSON对象中记录的文件，转换为PDF，输出到指定的目录中；回调应用系统接口，将数据写回。
     * @param parameters 输入的参数，JSON格式数据对象
     */
    JSONObject ConvertOffice(Map<String, Object> parameters);

}
