package com.thinkdifferent.convertpreview.utils;

import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class JpgWaterMarkUtil {

    /**
     * jpg 批量添加水印
     * @param sourceJpgList 图片list
     * @param convertEntity 输入参数
     * @throws Exception err
     */
    public static void mark4JpgList(List<String> sourceJpgList, ConvertEntity convertEntity) throws Exception {
        for (String strJpg : sourceJpgList) {
            mark4Jpg(strJpg, strJpg, convertEntity);
        }
    }


    /**
     * 给JPG添加水印, 单页
     *  @param strSourceJpg  源Jpg
     * @param strTargetJpg  目标Jpg
     * @param convertEntity 转换参数
     */
    public static void mark4Jpg(String strSourceJpg, String strTargetJpg, ConvertEntity convertEntity) throws Exception {
        //  如果添加图片水印，则进行如下处理
        if (convertEntity.getPngMark() != null) {
            convertEntity.getPngMark().mark4Jpg(strSourceJpg,
                    strTargetJpg,
                    convertEntity.getPngMark());
        }

        // 如果添加文字水印，则进行如下处理
        if (convertEntity.getTextMark() != null) {
            convertEntity.getTextMark().mark4Jpg(strSourceJpg,
                    strTargetJpg,
                    convertEntity.getTextMark(),
                    convertEntity.getAlpha());
        }

        // 如果添加归档章水印，则进行如下处理
        if (convertEntity.getFirstPageMark() != null) {
            convertEntity.getFirstPageMark().mark4Jpg(strSourceJpg,
                    strTargetJpg,
                    convertEntity.getFirstPageMark());
        }

    }
}
