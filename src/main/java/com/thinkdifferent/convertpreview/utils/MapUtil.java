package com.thinkdifferent.convertpreview.utils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class MapUtil {
    private MapUtil() {
    }

    public static String map2Str(Map<String, String> map) {
        return (String)map.keySet().stream().map((key) -> {
            return key + "=" + (String)map.get(key);
        }).collect(Collectors.joining(",", "{", "}"));
    }

    public static Map<String, String> str2Map(String mapAsString) {
        return (Map)Arrays.stream(mapAsString.substring(1, mapAsString.length() - 1).split(",")).map((entry) -> {
            return entry.split("=");
        }).collect(Collectors.toMap((entry) -> {
            return entry[0];
        }, (entry) -> {
            return entry[1];
        }));
    }
}
