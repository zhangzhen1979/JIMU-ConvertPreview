package com.thinkdifferent.convertpreview.config;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * 定时任务
 *
 * @author ltian
 * @version 1.0
 * @date 2022/12/16 10:49
 */
@Component
public class ArchiveScheduleConfig {
    /**
     * 清理临时目录下昨天的文件， 凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ? ")
    public void cleanTempDir() {
        // 输出目录
        cleanOldFile(new File(ConvertConfig.outPutPath));
        // 输入目录
        cleanOldFile(new File(ConvertConfig.inPutTempPath));
    }

    private void cleanOldFile(File dir) {
        File[] files = dir.listFiles(file -> file.isDirectory()
                && ReUtil.isMatch("\\d{4}-\\d{2}-\\d{2}", file.getName())
                && blnOldDate(file.getName()));
        if (Objects.nonNull(files)){
            Arrays.stream(files).forEach(File::delete);
        }

    }

    /**
     * 是否是以前的数据
     *
     * @param strDate 传入的文件名
     * @return bln
     */
    private boolean blnOldDate(String strDate) {
        try {
            return DateUtil.parse(strDate, DatePattern.NORM_DATE_FORMAT).isBefore(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
