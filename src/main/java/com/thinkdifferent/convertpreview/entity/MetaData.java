package com.thinkdifferent.convertpreview.entity;

import cn.hutool.core.map.MapUtil;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Data
public class MetaData {

    /**
     * 门类：financial（会计档案）；archive（文书/综合档案）
     */
    private String arcType;
    /**
     * 元数据类型：dat46、dat48、dat46-48、inv_ord_receiver……
     */
    private String configId;
    /**
     * 元数据JSON。
     */
    private JSONObject data;

    public static MetaData get(Map<String, Object> map) {
        MetaData metaData = new MetaData();
        metaData.setArcType(MapUtil.getStr(map, "arcType", null));
        metaData.setConfigId(MapUtil.getStr(map, "configId", null));
        metaData.setData(JSONObject.fromObject(map.get("data")));

        return StringUtils.isNotBlank(metaData.getArcType()) ?
                metaData : null;
    }

}
