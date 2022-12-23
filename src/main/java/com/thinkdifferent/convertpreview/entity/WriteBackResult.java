package com.thinkdifferent.convertpreview.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.cglib.beans.BeanMap;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/7/28 10:03
 */
@Data
@Accessors(chain = true)
public class WriteBackResult extends SimpleResult {
    /**
     * 文件路径
     */
    private String file;

    public WriteBackResult() {
        super.setFlag(false);
    }

    public WriteBackResult(boolean flag) {
        super.setFlag(flag);
    }

    public WriteBackResult(boolean flag, String message) {
        super(flag, message);
    }

    public WriteBackResult(boolean flag, String message, String file) {
        super(flag, message);
        this.file = file;
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
                ", file='" + file + '\'' +
                '}';
    }
}
