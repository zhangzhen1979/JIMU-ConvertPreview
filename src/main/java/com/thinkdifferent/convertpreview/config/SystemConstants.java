package com.thinkdifferent.convertpreview.config;

import cn.hutool.core.map.FixedLinkedHashMap;
import cn.hutool.crypto.SecureUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.thinkdifferent.convertpreview.entity.PdfEntity;
import net.sf.json.JSONObject;

import java.io.File;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 系统常量
 *
 * @author ltian
 * @version 1.0
 * @date 2022/7/20 16:33
 */
public interface SystemConstants {

    /**
     * json 重试 key
     */
    String RETRY_KEY = "currentRetryKey";

    /**
     * 文件临时存储目录, 按日期记录
     */
    String INPUT_FILE_PATH = System.getProperty("user.dir") + File.separator + "upload" + File.separator + LocalDate.now() + File.separator;
    /**
     * 系统对象缓存， 记录 12H， 最大 1W 条数据，清理时自动删除文件
     */
    LoadingCache<String, Optional<PdfEntity>> SYSTEM_PDF_ENTITY_CACHE = CacheBuilder.newBuilder()
            // 最大记录 10000 条
            .maximumSize(10000)
            // 缓存12小时
            .expireAfterWrite(12L, TimeUnit.HOURS)
            .removalListener((RemovalListener<String, Optional<PdfEntity>>) removalNotification -> {
                // 清理过期的文件
                Optional<PdfEntity> pdfEntity = removalNotification.getValue();
                if (Objects.nonNull(pdfEntity)){
                    pdfEntity.ifPresent(PdfEntity::clean);
                }
            })
            // 并发数
            .concurrencyLevel(10)
            .recordStats()
            // value 可不存在
            .build(new CacheLoader<String, Optional<PdfEntity>>() {
                @Override
                public Optional<PdfEntity> load(String key) {
                    return Optional.empty();
                }
            });

    /**
     * 固定长度队列，记录错误数据
     * key： 无重试次数的md5值
     * value： 含重试次数的数据
     */
    FixedLinkedHashMap<String, JSONObject> ERROR_CONVERT_DATA = new FixedLinkedHashMap<>(200);

    /**
     * 添加错误数据
     * @param data  错误数据，含重试记录
     */
    static void addErrorData(JSONObject data) {
        JSONObject joKey = JSONObject.fromObject(data);
        joKey.put(RETRY_KEY, "0");
        ERROR_CONVERT_DATA.put(SecureUtil.md5(joKey.toString()), data);
    }

    /**
     * 移除错误数据
     * @param data  需移除错误数据，可能有重试次数记录
     */
    static void removeErrorData(JSONObject data) {
        JSONObject joKey = JSONObject.fromObject(data);
        joKey.put(RETRY_KEY, "0");
        ERROR_CONVERT_DATA.remove(SecureUtil.md5(joKey.toString()));
    }
}
