package com.thinkdifferent.convertpreview.config;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Objects;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/1/30 15:49
 */
@Slf4j
@Component
public class ScheduleConfig {

    /**
     * 定时移除未清理的文件，每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ? ")
    public void cleanPdfImages() {
        String strInputTempPath = ConvertConfig.inPutTempPath;
        String strOutputTempPath = ConvertConfig.outPutPath;
        String[] strTempPaths = {strInputTempPath, strOutputTempPath};

        // 当前时间 - 24 * 3
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DATE, -3);
        long day3Ago = calendar1.getTime().getTime();

        // 可删除的文件
        for (String strTempPath : strTempPaths) {
            File[] files = new File(strTempPath).listFiles(pathname -> canDelete(pathname, day3Ago));
            if (Objects.nonNull(files)) {
                for (File file : files) {
                    try {
                        FileUtil.del(file);
                    } catch (Exception e) {
                        log.error("移除过期文件失败：" + file.getName(), e);
                    }
                }
            }
        }

    }

    /**
     * 判断文件夹是否可删除
     *
     * @param file    需要判断的文件
     * @param day3Ago 3天前的时间，判断文件夹创建时间是否小于此时间，小于删除
     * @return 判断文件夹创建时间是否小于3天前的时间
     */
    public static boolean canDelete(File file, long day3Ago) {
        try {
            long t = Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toMillis();
            return day3Ago > t;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
