package com.thinkdifferent.convertpreview.engine.impl.onlyOffice.vo.thumbnail;

import lombok.Data;

/**
 * @BelongsProject: leaf-onlyoffice
 * @BelongsPackage: com.ideayp.leaf.onlyoffice.dto.convert.thumbnail
 * @Author: TongHui
 * @CreateTime: 2022-11-23 10:18
 * @Description: 缩略图 定义将图像格式（bmp，gif，jpg，png）指定为输出类型时缩略图的设置。
 * @Version: 1.0
 */
@Data
public class Thumbnail {
    /**
     * 定义使图像适合指定高度和宽度的模式。 支持的值：
     * 0-拉伸文件以适合高度和宽度，
     * 1-保留图像的纵横比，
     * 2-在这种情况下，不使用宽度和高度设置。 取而代之的是，页面的度量大小将转换为具有 96dpi 的像素。 例如，A4（210x297mm）页面将是一张尺寸为794x1123像素的图片。
     * 默认值为2。
     */
    private Integer aspect;
    /**
     * 定义是应仅为第一页生成缩略图，还是应为所有文档页面生成缩略图。
     * 如果为 false，则将创建包含所有页面缩略图的 zip 存档。
     * 默认值为true，
     */
    private Boolean first;
    /**
     * 定义缩略图高度（以像素为单位）。 默认值为100。
     */
    private Integer height;
    /**
     * 定义缩略图宽度（以像素为单位）。 默认值为100。
     */
    private Integer width;
}
