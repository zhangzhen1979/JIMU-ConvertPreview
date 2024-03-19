package com.thinkdifferent.convertpreview.task;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class DeleteTempFileTask {

    /**
     * 清理临时目录的间隔时间，单位：天
     */
    @Value("${convert.path.cleanUnit:1}")
    private Integer cleanUnit;

    /**
     * 定时移除未清理的文件，每天凌晨1点执行。删除创建时间为[cleanUnit]天前的文件。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanTempFile() {
        String strInputTempPath = ConvertDocConfigBase.inPutTempPath;
        String strOutputTempPath = ConvertDocConfigBase.outPutPath;
        String[] strTempPaths = {strInputTempPath, strOutputTempPath};

        // 当前时间 - 24 * cleanUnit
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DATE, -cleanUnit);
        long dayAgo = calendar1.getTime().getTime();

        // 可删除的文件
        for (String strTempPath : strTempPaths) {
            File[] files = new File(strTempPath).listFiles(pathname -> canDelete(pathname, dayAgo));
            if (Objects.nonNull(files)) {
                for (File file : files) {
                    try {
                        FileUtil.del(file);
                        log.debug("移除过期文件：" + file.getName());
                    } catch (Exception | Error e) {
                        log.error("移除过期文件失败：" + file.getName(), e);
                    }
                }
            }
        }

    }

    /**
     * 判断文件夹是否可删除
     *
     * @param file   需要判断的文件
     * @param dayAgo N天前的时间，判断文件夹创建时间是否小于此时间，小于删除
     * @return 判断文件夹创建时间是否小于N天前的时间
     */
    public static boolean canDelete(File file, long dayAgo) {
        try {
            long t = Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toMillis();
            return dayAgo > t;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
