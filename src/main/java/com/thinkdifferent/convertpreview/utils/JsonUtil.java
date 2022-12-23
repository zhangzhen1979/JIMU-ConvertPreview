package com.thinkdifferent.convertpreview.utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author ltian
 * @version 1.0
 * @date 2021/11/16 16:30
 */
public class JsonUtil {

    private static Gson gson = null;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        builder.registerTypeAdapter(Date.class, new DateSerializer()).setDateFormat(DateFormat.LONG);
        builder.registerTypeAdapter(Date.class, new DateDeserializer()).setDateFormat(DateFormat.LONG);
        gson = builder.create();
    }

    /**
     * 将对象转换为json字符串
     *
     * @param object 要序列化的实体
     * @return 转换后的字符串
     */
    public static String toJSONString(Object object) {
        return gson.toJson(object);
    }

    /**
     * 将json转换为对应的实体
     *
     * @param json json字符串
     * @param type 对应实体类型
     * @param <T>  返回的实体类型
     * @return 返回对应实体
     */
    public static <T> T parseObject(String json, Type type) {
        return gson.fromJson(json, type);
    }

}

/**
 * json序列化支持data与long类型的转换
 */
class DateSerializer implements JsonSerializer<Date> {
    @Override
    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getTime());
    }
}

/**
 * json饭序列化支持data与long类型的转换
 */
class DateDeserializer implements JsonDeserializer<Date> {
    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return new Date(json.getAsJsonPrimitive().getAsLong());
    }

}
