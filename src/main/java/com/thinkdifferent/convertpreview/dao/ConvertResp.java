package com.thinkdifferent.convertpreview.dao;

import cn.hutool.core.exceptions.ExceptionUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 接口返回对象
 *
 * @author ltian
 * @version 1.0
 * @date 2023/11/29 15:06
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class ConvertResp {
    private Boolean flag;
    private String message;
    private Integer code;

    public static ConvertResp success() {
        return success("");
    }

    public static ConvertResp success(String message) {
        return new ConvertResp()
                .setCode(200)
                .setMessage(message)
                .setFlag(true);
    }

    public static ConvertResp failed(Exception e) {
        return failed(ExceptionUtil.getMessage(e));
    }

    public static ConvertResp failed(String message) {
        return new ConvertResp()
                .setCode(500)
                .setMessage(message)
                .setFlag(false);
    }
}
