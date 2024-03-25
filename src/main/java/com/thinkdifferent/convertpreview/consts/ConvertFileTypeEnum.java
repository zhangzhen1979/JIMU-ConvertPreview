package com.thinkdifferent.convertpreview.consts;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineBase;
import com.thinkdifferent.convertpreview.service.PoiConvertTypeService;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 转换文件类型
 *
 * @author ltian
 * @version 1.0
 * @date 2023/12/28 17:02
 */
public enum ConvertFileTypeEnum {
    // 这种方式会设置null
    img(ConvertDocConfigEngineBase.picType),
    pdf("pdf"),
    ofd("ofd"),
    html("html,htm"),
    cad("dxf,dwg"),
    word("doc,docx"),
    excel("xls,xlsx"),
    zip("zip,rar,7z"),
    xml("xml"),
    ppt("ppt,pptx"),
    /**
     * 默认处理
     */
    engine(""),
    ;

    private final String type;

    ConvertFileTypeEnum(String type) {
        this.type = type;
    }

    public static ConvertFileTypeEnum valueOfExtName(String extName) {
        // 默认的处理方法
        ConvertFileTypeEnum fileType = engine;

        for (ConvertFileTypeEnum fileTypeEnum : ConvertFileTypeEnum.values()) {
            if (img == fileTypeEnum) {
                if (Arrays.asList(ConvertDocConfigEngineBase.picType.split(",")).contains(extName)) {
                    fileType = img;
                    break;
                }
            } else if (Arrays.asList(fileTypeEnum.type.split(",")).contains(extName)) {
                fileType = fileTypeEnum;
                break;
            }
        }
        // 非 poi 转换的，excel、ppt、word 走引擎转换
        try {
            if (fileType.equals(excel) || fileType.equals(ppt) || fileType.equals(word)) {
                List<String> poiService = SpringUtil.getBeansOfType(PoiConvertTypeService.class)
                        .keySet()
                        .stream()
                        .map(beanName -> StringUtils.substring(beanName, 7, -11).toLowerCase())
                        .collect(Collectors.toList());

                boolean isPoi = false;
                if (CollectionUtil.isNotEmpty(poiService)) {
                    for (String simpleServiceName : poiService) {
                        if (simpleServiceName.startsWith(fileType.name())) {
                            isPoi = true;
                            break;
                        }
                    }
                }
                if (!isPoi) {
                    fileType = engine;
                }
            }
        } catch (Exception ignored) {
        }

        // 首字母大写
        return fileType;
    }

    /**
     * 获取文件处理类型
     *
     * @param file 传入文件
     * @return 处理类型，首字母大写
     */
    public static String getType(File file) {
        String extName = FileUtil.extName(file);
        return valueOfExtName(extName).name();
    }
}
