package com.thinkdifferent.convertpreview.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import net.sf.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/4/18 9:25
 */
@Data
@Accessors(chain = true)
public class PdfCoverVO implements Serializable {
    /**
     * 是否只读
     */
    private boolean outPutReadOnly;
    /**
     * 回调地址
     */
    private String callBackUrl;
    /**
     * 回写类型
     *
     * @see com.thinkdifferent.convertpreview.entity.WriteBackType
     */
    private String writeBackType;
    /**
     * 回写地址
     */
    private JSONObject writeBack;
    /**
     * 回写文件名（无扩展名）
     */
    private String outPutFileName;
    /**
     * 回写请求头，
     */
    private JSONObject writeBackHeaders;

    public Map<String, Object> toWriteBackMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("writeBack", writeBack);
        result.put("writeBackHeaders", writeBackHeaders);
        return result;
    }
}
