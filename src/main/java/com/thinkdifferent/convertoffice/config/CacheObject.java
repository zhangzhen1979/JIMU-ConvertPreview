package com.thinkdifferent.convertoffice.config;

import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@Component
public class CacheObject {

    // PNG水印图片缓存变量。key为文件名（文字的MD5值），Value为文件的缓存对象。
    public static Map<String, BufferedImage> mapPng = new HashMap<>();

}
