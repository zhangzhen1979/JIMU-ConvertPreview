package com.thinkdifferent.convertpreview.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/7/29 16:48
 */
@Data
@NoArgsConstructor
public class ConvertBase64Entity extends CallBackResult {
    private List<SimpleBase64> listBase64;

    public ConvertBase64Entity(CallBackResult result) {
        super.setFlag(result.isFlag()).setMessage(result.getMessage());
        super.setFilePath(result.getFilePath());
        this.setListBase64(Collections.emptyList());
    }

    @Override
    public String toString() {
        return "ConvertBase64Entity{" +
                "flag=" + super.isFlag() +
                ", message='" + super.getMessage() + '\'' +
                ", filePath='" + super.getFilePath() + '\'' +
                ", listBase64=" + listBase64 +
                '}';
    }

    @Data
    @AllArgsConstructor
    public static class SimpleBase64{
        private String filename;
        private String base64;
    }
}
