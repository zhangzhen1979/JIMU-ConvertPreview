package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import net.sf.json.JSONObject;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * 预览缓存类
 */
public class OnlineCacheUtil {
    private OnlineCacheUtil() {
    }

    private static final Cache<String, File> PREVIEW_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(8))
            .build();

    private static final String TEMP = "temp";

    public static void put(String key, File value) {
        put(key, "", value);
    }

    public static void putTemp(String key, File value) {
        put(key, TEMP, value);
    }

    private static void put(String key, String temp, File value) {
        PREVIEW_CACHE.put(key + "::" + temp, value);
    }

    public static void putTemp(File file, String outPutFileType, File value){
        JSONObject joInput = new JSONObject();
        joInput.put("inputFile", FileUtil.getCanonicalPath(file));
        joInput.put("inputFileType", FileUtil.extName(file));
        joInput.put("outPutFileType", outPutFileType);
        putTemp(joInput.toString(), value);
    }

    public static File get(String key) {
        File result = PREVIEW_CACHE.getIfPresent(key + "::");
        if (!FileUtil.exist(result)) {
            result = PREVIEW_CACHE.getIfPresent(key + "::" + TEMP);
        }
        if (!FileUtil.exist(result)){
            return null;
        }
        return result;
    }

    @SneakyThrows
    public static File get(String key, Callable<File> callable) {
        File result = get(key);
        if (!FileUtil.exist(result)){
            File callResult = callable.call();
            put(key, callResult);
            return callResult;
        }
        return result;
    }
}
