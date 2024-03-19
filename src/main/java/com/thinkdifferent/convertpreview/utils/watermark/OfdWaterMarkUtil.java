package com.thinkdifferent.convertpreview.utils.watermark;

import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.ReUtil;
import com.thinkdifferent.convertpreview.entity.ConvertDocEntity;
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
     * @param strSourceOfd      源OFD
     * @param strTargetOfd      目标OFD
     * @param convertDocEntity  转换参数
     * @param intPageNum        上一个文件的最后页码
     * @param intPageCount      截止到上一个文件的最后总页数
     * @throws Exception
     */
    public static void mark4Ofd(String strSourceOfd,
                                String strTargetOfd,
                                ConvertDocEntity convertDocEntity,
                                int intPageNum,
                                int intPageCount) throws Exception {
        Path pathInput = Paths.get(strSourceOfd);
        Path pathOutput = Paths.get(strTargetOfd);

        @Cleanup OFDReader reader = new OFDReader(pathInput);
        @Cleanup OFDDoc ofdDoc = new OFDDoc(reader, pathOutput);

        int intPages = reader.getNumberOfPages();

        float h = 0f;
        if (convertDocEntity.getTextMark() != null) {
            // 使用"||"将内容进行分割
            String[] strWaterMarkTexts = convertDocEntity.getTextMark().getWaterMarkText().split("\\|\\|");
            List<String> listWaterMark = Arrays.asList(strWaterMarkTexts);

            int maxSize = Math.max(listWaterMark.stream().mapToInt(str -> {
                        // 汉字算一个字，其他字长度算半个
                        int chineseNum = ReUtil.count(PatternPool.CHINESE, str);
                        return chineseNum + (str.length() - chineseNum) / 2;
                    }).max().orElse(2),
                    2 * listWaterMark.size());

            h = maxSize * convertDocEntity.getTextMark().getFontSize();
        }

        for (int i = 1; i <= intPages; i++) {
            // 获取每页尺寸
            Page page = reader.getPage(i);
            ST_Box pageSize = reader.getPageSize(page);

            // 如果添加图片水印，则进行如下处理
            if (convertDocEntity.getPngMark() != null) {
                convertDocEntity.getPngMark().mark4Ofd(ofdDoc,
                        pageSize,
                        convertDocEntity.getPngMark(),
                        null,
                        i,
                        convertDocEntity.getAlpha());
            }

            // 如果添加页码，则进行如下处理
            if (convertDocEntity.getPageNum() != null &&
                    convertDocEntity.getPageNum().isEnable()) {
                // 如果设置为单个文件single，且输入文件也是单个，则加页码
                if(("single".equalsIgnoreCase(convertDocEntity.getPageNum().getType()) &&
                        convertDocEntity.getInputFiles().size() == 1)||
                        // 如果设置为多个文件mutli，且输入文件也是多个，则加页码
                        ("multi".equalsIgnoreCase(convertDocEntity.getPageNum().getType()) &&
                                convertDocEntity.getInputFiles().size() > 1)||
                        // 如果设置为所有all，则加页码
                        ("all".equalsIgnoreCase(convertDocEntity.getPageNum().getType()))||
                        // 如果不设置，则为所有，加页码
                        (convertDocEntity.getPageNum().getType() == null ||
                                "".equalsIgnoreCase(convertDocEntity.getPageNum().getType()))){
                    intPageNum++;
                    intPageCount++;
                    convertDocEntity.getPageNum().addPageNum4Ofd(ofdDoc,
                            pageSize,
                            convertDocEntity.getPageNum(),
                            i,
                            intPageNum,
                            intPageCount);
                }
                // 否则，不加页码

            }

            // 如果添加【版权声明】，则进行如下处理
            if (convertDocEntity.getCopyRight() != null &&
                    convertDocEntity.getCopyRight().isEnable()) {
                // 如果设置为单个文件single，且输入文件也是单个，则加页码
                if(("single".equalsIgnoreCase(convertDocEntity.getCopyRight().getType()) &&
                        convertDocEntity.getInputFiles().size() == 1)||
                        // 如果设置为多个文件mutli，且输入文件也是多个，则加页码
                        ("multi".equalsIgnoreCase(convertDocEntity.getCopyRight().getType()) &&
                                convertDocEntity.getInputFiles().size() > 1)||
                        // 如果设置为所有all，则加页码
                        ("all".equalsIgnoreCase(convertDocEntity.getCopyRight().getType()))||
                        // 如果不设置，则为所有，加页码
                        (convertDocEntity.getCopyRight().getType() == null ||
                                "".equalsIgnoreCase(convertDocEntity.getCopyRight().getType()))){
                    intPageNum++;
                    intPageCount++;
                    convertDocEntity.getCopyRight().addCopyRight4Ofd(ofdDoc,
                            pageSize,
                            convertDocEntity.getCopyRight(),
                            i,
                            intPageCount);
                }
                // 否则，不加页码

            }

            // 如果添加文字水印，则进行如下处理
            if (convertDocEntity.getTextMark() != null) {
                convertDocEntity.getTextMark().mark4Ofd(ofdDoc,
                        pageSize,
                        convertDocEntity.getTextMark(),
                        i,
                        convertDocEntity.getAlpha());
            }

            // 如果添加归档章水印，则进行如下处理
            if (i == 1 && convertDocEntity.getFirstPageMark() != null) {
                convertDocEntity.getFirstPageMark().mark4Ofd(ofdDoc,
                        pageSize,
                        convertDocEntity.getFirstPageMark(),
                        i,
                        1f);
            }

            // 如果添加二维码/条码，则进行如下处理
            if (convertDocEntity.getBarCode() != null) {
                if(convertDocEntity.getBarCode().getIsFirstPage() && i>1){
                    // 如果设置只在首页添加，但是当前页面不是第一页，则跳过。
                    continue;
                }else{
                    convertDocEntity.getBarCode().mark4Ofd(ofdDoc,
                            pageSize,
                            convertDocEntity.getBarCode(),
                            i,
                            1f);
                }
            }
        }
    }


    /**
     * 计算单行文本宽度
     * @param text       单行文本的内容
     * @param fontSize   字号
     * @return           宽度值
     */
    public static double getTextWidth(String text,float fontSize){
        return fontSize * 1.5 * (text.length() / 2);
    }

    /**
     * 计算多行文本宽度
     * @param text       多行文本的内容
     * @param fontSize   字号
     * @return           最长一行的宽度值
     */
    public static double getTextsWidth(String text,float fontSize){
        String[] strWaterMarkTexts = text.split("\\n");
        int intMax = 0;
        for(int i=0;i<strWaterMarkTexts.length;i++){
            if(strWaterMarkTexts[i].length() > intMax){
                intMax = strWaterMarkTexts[i].length();
            }
        }

        return fontSize * 1.5 * (intMax / 2);
    }


}
