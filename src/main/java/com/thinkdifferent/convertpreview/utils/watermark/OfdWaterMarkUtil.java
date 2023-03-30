package com.thinkdifferent.convertpreview.utils.watermark;

import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.ReUtil;
import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class OfdWaterMarkUtil {

    /**
     * 给OFD添加水印
     *  @param strSourceOfd  源OFD
     * @param strTargetOfd  目标OFD
     * @param convertEntity 转换参数
     */
    public static void mark4Ofd(String strSourceOfd,
                                String strTargetOfd,
                                ConvertEntity convertEntity) throws Exception {
        int intPageNum = 0;
        Path pathInput = Paths.get(strSourceOfd);
        Path pathOutput = Paths.get(strTargetOfd);

        @Cleanup OFDReader reader = new OFDReader(pathInput);
        @Cleanup OFDDoc ofdDoc = new OFDDoc(reader, pathOutput);

        int intPages = reader.getNumberOfPages();

        float h = 0f;
        if (convertEntity.getTextMark() != null) {
            // 使用"||"将内容进行分割
            String[] strWaterMarkTexts = convertEntity.getTextMark().getWaterMarkText().split("\\|\\|");
            List<String> listWaterMark = Arrays.asList(strWaterMarkTexts);

            int maxSize = Math.max(listWaterMark.stream().mapToInt(str -> {
                        // 汉字算一个字，其他字长度算半个
                        int chineseNum = ReUtil.count(PatternPool.CHINESE, str);
                        return chineseNum + (str.length() - chineseNum) / 2;
                    }).max().orElse(2),
                    2 * listWaterMark.size());

            h = maxSize * convertEntity.getTextMark().getFontSize();
        }

        for (int i = 1; i <= intPages; i++) {
            // 获取每页尺寸
            Page page = reader.getPage(i);
            ST_Box pageSize = reader.getPageSize(page);

            // 如果添加图片水印，则进行如下处理
            if (convertEntity.getPngMark() != null) {
                convertEntity.getPngMark().mark4Ofd(ofdDoc,
                        pageSize,
                        convertEntity.getPngMark(),
                        i,
                        convertEntity.getAlpha());
            }

            // 如果添加页码，则进行如下处理
            if (convertEntity.isPageNum()) {
                intPageNum++;
                convertEntity.getTextMark().addPageNum4Ofd(ofdDoc,
                        pageSize,
                        i,
                        intPageNum);
            }

            // 如果添加文字水印，则进行如下处理
            if (convertEntity.getTextMark() != null) {
                convertEntity.getTextMark().mark4Ofd(ofdDoc,
                        pageSize,
                        convertEntity.getTextMark(),
                        i, h,
                        convertEntity.getAlpha());
            }

            // 如果添加归档章水印，则进行如下处理
            if (i == 1 && convertEntity.getFirstPageMark() != null) {
                convertEntity.getFirstPageMark().mark4Ofd(ofdDoc,
                        pageSize,
                        convertEntity.getFirstPageMark(),
                        i,
                        1f);
            }

            // 如果添加二维码/条码，则进行如下处理
            if (convertEntity.getBarCode() != null) {
                if(convertEntity.getBarCode().getIsFirstPage() && i>1){
                    // 如果设置只在首页添加，但是当前页面不是第一页，则跳过。
                    continue;
                }else{
                    convertEntity.getBarCode().mark4Ofd(ofdDoc,
                            pageSize,
                            convertEntity.getBarCode(),
                            i,
                            1f);
                }
            }
        }
    }


}
