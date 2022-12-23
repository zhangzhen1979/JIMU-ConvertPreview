package com.thinkdifferent.convertpreview.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/7/28 9:35
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class SimpleResult {
    private boolean flag;
    private String message;

    @Override
    public String toString() {
        return "SimpleResult{" +
                "flag=" + flag +
                ", message='" + message + '\'' +
                '}';
    }
}
