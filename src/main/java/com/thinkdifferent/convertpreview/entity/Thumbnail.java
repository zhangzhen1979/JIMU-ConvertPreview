package com.thinkdifferent.convertpreview.entity;

import cn.hutool.core.map.MapUtil;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Objects;

public class Thumbnail {

    private int width;
    private int height;
    private double scale;
    private double quality;

    public static Thumbnail convert(Map<String, Object> parameters){
        Object thumbnailParams = parameters.get("thumbnail");
        if (Objects.isNull(thumbnailParams)) {
            return null;
        }
        Assert.isTrue(thumbnailParams instanceof Map, "缩略图信息格式错误");

        Map<String, Object> mapThumbnail = (Map<String, Object>) thumbnailParams;

        Thumbnail thumbnail = new Thumbnail();
        thumbnail.setWidth(MapUtil.getInt(mapThumbnail, "width", 0));
        thumbnail.setHeight(MapUtil.getInt(mapThumbnail, "height", 0));
        thumbnail.setScale(MapUtil.getDouble(mapThumbnail, "scale", 0d));
        thumbnail.setQuality(MapUtil.getDouble(mapThumbnail, "quality", 0d));

        return thumbnail;
    }


    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

}
