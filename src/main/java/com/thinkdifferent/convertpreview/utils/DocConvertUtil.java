package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.entity.ConvertDocEntity;
import com.thinkdifferent.convertpreview.entity.TargetFile;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.ofdrw.tool.merge.OFDMerger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class DocConvertUtil {
    /**
     * 如果设置了输出指定页数，则进行执行页数的处理
     *
     * @param convertDocEntity 传入对象
     * @param fileTarget       合并后文件
     * @param longPageCount    文件的原始页数
     * @return 页数截取后文件内容
     */
    @SneakyThrows
    public static TargetFile cutFile(ConvertDocEntity convertDocEntity, File fileTarget, long longPageCount) {
        TargetFile targetFile = new TargetFile();
        // 如果设置了输出指定页数，则进行执行页数的处理
        if (StringUtils.isNotBlank(convertDocEntity.getPageLimits())) {
            List<Integer> listPages = SystemUtil.getPages(convertDocEntity.getPageLimits());
            if (!listPages.isEmpty()) {
                longPageCount = listPages.size();
                File fileCut = null;
                if ("pdf".equalsIgnoreCase(convertDocEntity.getOutPutFileType())) {
                    // 将指定页输出为单页pdf
                    @Cleanup PDDocument doc = Loader.loadPDF(fileTarget);
                    fileCut = new File(fileTarget.getAbsolutePath() + "_cut.pdf");
                    @Cleanup PDDocument docCut = new PDDocument();
                    int pageCount = doc.getNumberOfPages();
                    for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                        if (!listPages.isEmpty() && !listPages.contains(pageIndex + 1)) {
                            // 如果设置了指定页转换，并且当前页不在控制列表中，则跳过，不转换。
                            continue;
                        }
                        PDPage page = doc.getPages().get(pageIndex);
                        docCut.addPage(page);
                    }
                    docCut.save(fileCut);
                } else if ("ofd".equalsIgnoreCase(convertDocEntity.getOutPutFileType())) {
                    Path doc = Paths.get(fileTarget.getAbsolutePath());
                    fileCut = new File(fileTarget.getAbsolutePath() + "_cut.ofd");
                    Path docCut = Paths.get(fileCut.getAbsolutePath());
                    try (OFDMerger ofdMerger = new OFDMerger(docCut)) {
                        int[] integers = listPages.stream().filter(Objects::nonNull).mapToInt(i -> i).toArray();
                        ofdMerger.add(doc, integers);
                    }
                }
                if (Objects.nonNull(fileCut)) {
                    FileUtil.move(fileCut, fileTarget, true);
                }
            }
        }

        targetFile.setTarget(fileTarget);
        targetFile.setLongPageCount(longPageCount);

        return targetFile;
    }



}
