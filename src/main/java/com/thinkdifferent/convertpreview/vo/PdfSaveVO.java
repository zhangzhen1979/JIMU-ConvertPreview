package com.thinkdifferent.convertpreview.vo;

import lombok.Data;

import java.util.Map;

/**
 * 保存修改后的数据
 * @author ltian
 * @version 1.0
 * @date 2022/7/12 15:53
 */
@Data
public class PdfSaveVO {
    /**
     * 唯一主键
     */
    private String uuid;
    /**
     * 修改后的数据
     */
    private Map<String, String> data;
}
