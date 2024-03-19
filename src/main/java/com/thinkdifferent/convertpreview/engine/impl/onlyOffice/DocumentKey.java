package com.thinkdifferent.convertpreview.engine.impl.onlyOffice;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;

import java.util.Calendar;
import java.util.Random;

/**
 * @BelongsProject: office-demo
 * @BelongsPackage: com.doc.utils
 * @Author: TongHui
 * @CreateTime: 2022-11-04 18:25
 * @Description: 生成ID
 * @Version: 1.0
 */

public class DocumentKey {

    private static Snowflake snowflake;

    static {
        // 0 ~ 31 位，可以采用配置的方式使用
        long workerId;
        try {
            workerId = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr());
        } catch (Exception | Error e) {
            workerId = NetUtil.getLocalhostStr().hashCode();
        }

        workerId = workerId >> 16 & 31;

        long dataCenterId = 1L;
        snowflake = IdUtil.createSnowflake(workerId, dataCenterId);
    }

    public static String SnowflakeId() {
        return String.valueOf(snowflake.nextId());
    }

    public static String ShortCode() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        // 打乱排序：2020年为准 + 小时 + 周期 + 日 + 三位随机数
        StringBuilder idStr = new StringBuilder();
        idStr.append(year - 2020);
        idStr.append(hour);
        //(“%04d”, 99)	0099
        idStr.append(String.format("%02d", week));
        idStr.append(day);
        idStr.append(String.format("%03d", new Random().nextInt(1000)));

        return idStr.toString();
    }
}
