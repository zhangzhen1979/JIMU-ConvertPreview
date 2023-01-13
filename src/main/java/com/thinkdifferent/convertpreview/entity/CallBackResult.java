package com.thinkdifferent.convertpreview.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.cglib.beans.BeanMap;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/7/28 10:03
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class CallBackResult extends SimpleResult {
    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件Base64值
     */
    private String base64;

    /**
     * HTTP响应对象
     */
    private HttpServletResponse response;

    public CallBackResult(boolean flag) {
        super.setFlag(flag);
    }

    public CallBackResult(boolean flag, String message) {
        super(flag, message);
    }

    public CallBackResult(boolean flag, String message, String filePath) {
        super(flag, message);
        this.filePath = filePath;
    }

    public Map<String, Object> bean2Map() {
        BeanMap beanMap = BeanMap.create(this);
        Map<String, Object> map = new HashMap<>();

        beanMap.forEach((key, value) -> map.put(String.valueOf(key), value));
        return map;
    }


    @Override
    public String toString() {
        return "WriteBackResult{" +
                "flag=" + super.isFlag() +
                ", message='" + super.getMessage() + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
