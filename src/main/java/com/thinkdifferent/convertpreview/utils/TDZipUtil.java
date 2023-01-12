package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import lombok.extern.log4j.Log4j2;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/12/27 18:29
 */
@Log4j2
public class TDZipUtil {

    /**
     * 7z解压压缩包
     *
     * @param zipFile       7z文件
     * @param targetFileDir 目标文件夹
     * @param password      解压密码
     * @return 解压后文件夹
     * @throws IOException err
     */
    public static File unzip(File zipFile, String targetFileDir, String password) throws IOException {
        if (Objects.isNull(password)) {
            password = "";
        }
        // 保存目录带压缩包文件名
        if (!(targetFileDir.endsWith(zipFile.getName()) || targetFileDir.endsWith(zipFile.getName() + "/"))) {
            targetFileDir = Paths.get(targetFileDir, zipFile.getName()).toFile().getCanonicalPath();
        }
        //1.判断压缩文件是否存在，以及里面的内容是否为空
        if (!zipFile.exists()) {
            log.info(">>>>>>压缩文件【" + zipFile.getName() + "】不存在<<<<<<");
            return null;
        } else if (0 == zipFile.length()) {
            log.info(">>>>>>压缩文件【" + zipFile.getName() + "】大小为0不需要解压<<<<<<");
            return null;
        } else if (FileUtil.exist(targetFileDir) && Objects.requireNonNull(FileUtil.file(targetFileDir).listFiles()).length > 0) {
            // 文件已解压，直接返回
            return new File(targetFileDir);
        } else {
            //7zip 解压文件
            return unzipFile_(zipFile, targetFileDir, password);
        }
    }

    private static File unzipFile_(File zipFile, String targetFileDir, String password) {
        RandomAccessFile randomAccessFile = null;
        IInArchive inArchive = null;
        try {
            // 判断目标目录是否存在，不存在则创建
            File newdir = new File(targetFileDir);
            if (!newdir.exists()) {
                newdir.mkdirs();
            }
            randomAccessFile = new RandomAccessFile(zipFile, "r");
            RandomAccessFileInStream t = new RandomAccessFileInStream(randomAccessFile);
            inArchive = SevenZip.openInArchive(ArchiveFormat.ZIP, t);
            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
            for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                if (!item.isFolder()) {
                    ExtractOperationResult result;
                    result = item.extractSlow(data -> {
                        //写入指定文件
                        FileOutputStream fos1 = null;
                        try {
                            if (item.getPath().indexOf(File.separator) > 0) {
                                String path = targetFileDir + File.separator + item.getPath().substring(0, item.getPath().lastIndexOf(File.separator));
                                File folderExisting = new File(path);
                                if (!folderExisting.exists())
                                    new File(path).mkdirs();
                            }

                            fos1 = new FileOutputStream(targetFileDir + File.separator + item.getPath(), true);
                            log.debug(">>>>>>保存文件至：" + targetFileDir + File.separator + item.getPath());
                            fos1.write(data);
                            fos1.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (Objects.nonNull(fos1)) {
                                try {
                                    fos1.close();
                                } catch (IOException ignored) {
                                }
                            }
                        }
                        return data.length; // Return amount of consumed data
                    }, password);

                    if (result != ExtractOperationResult.OK) {
                        log.error("压缩包提取文件失败: {}, file:{}", result, item.getPath());
                    }
                }
            }
            return newdir;
        } catch (Exception e) {
            log.error("压缩包解压失败: " + e);
            return null;
        } finally {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (SevenZipException ignored) {
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException ignored) {
                }
            }
        }
    }


}
