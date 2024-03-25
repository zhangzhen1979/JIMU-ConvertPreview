package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.system.OsInfo;
import com.thinkdifferent.convertpreview.cache.CacheManager;
import com.thinkdifferent.convertpreview.config.ConvertVideoConfig;
import com.thinkdifferent.convertpreview.entity.ConvertVideoEntity;
import com.thinkdifferent.convertpreview.entity.Thumbnail;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

@Log4j2
public class ConvertVideoUtils {

    private String strInputPath;
    private ConvertVideoEntity convertVideoEntity;
    private String strExt;

    public ConvertVideoUtils(String strInputPath, ConvertVideoEntity convertVideoEntity) {
        this.strInputPath = strInputPath;
        this.convertVideoEntity = convertVideoEntity;
    }

    public String getExt() {
        return this.strExt;
    }

    public Boolean setVoidInfos() {
        if (!FileUtil.isFile(strInputPath)) {
            log.error(strInputPath + " is not file");
            return false;
        }
        if (process(strInputPath, convertVideoEntity)) {
            log.info("ok");
            return true;
        }
        log.error(strInputPath + " process error");
        return false;
    }


    public boolean process(String strInputPath, ConvertVideoEntity convertVideoEntity) {
        log.info("启动音视频转换进程");
        boolean blnStatus = processConvert(strInputPath, convertVideoEntity);
        return blnStatus;
    }


    @SneakyThrows
    private boolean processConvert(String strOldFilePath, ConvertVideoEntity convertVideoEntity) {
        if (!FileUtil.isFile(strOldFilePath)) {
            log.info(strOldFilePath + " is not file");
            return false;
        }
        strOldFilePath = SystemUtil.beautifulFilePath(strOldFilePath);
        String strInputFileType = FileUtil.extName(new File(strOldFilePath)).toLowerCase();

        LineMaker lineMaker = new LineMaker();

        List<String> listCommand = new ArrayList<>();
        listCommand.add(ConvertVideoConfig.ffmpegFile);

        String strFileName;

        String strOutputPath = SystemUtil.beautifulPath(convertVideoEntity.getWriteBack().getOutputPath());


        // 设置音频扩展名
        String[] strAudioExt = {"3gp", "3gpp", "amr", "aac", "ape", "aif", "au",
                "mid", "wma", "ra", "rm", "rmx", "vqf", "ogg", "wav", "m4a"};
        ArrayList<String> listAudioExt = new ArrayList<>(strAudioExt.length);
        Collections.addAll(listAudioExt, strAudioExt);

        if (listAudioExt.contains(strInputFileType)) {
            // 如果输入的是音频文件，则转换生成mp3
            this.strExt = "mp3";
            strFileName = strOutputPath + convertVideoEntity.getOutPutFileName() + ".mp3";
            lineMaker.toMp3ByParam(listCommand, strOldFilePath, strFileName);
        } else {
            // 否则是视频文件，生成MP4或对视频截图生成JPG
            // 判断是转MP4还是截图JPG
            if (!StringUtils.isEmpty(convertVideoEntity.getJpgFileName())) {
                // 截屏生成JPG
                this.strExt = "jpg";
                strFileName = strOutputPath + convertVideoEntity.getJpgFileName() + ".jpg";
                lineMaker.toJpg(listCommand, convertVideoEntity, strOldFilePath, strFileName);

            } else {
                // 转换生成MP4
                this.strExt = "mp4";
                strFileName = strOutputPath + convertVideoEntity.getOutPutFileName() + ".mp4";

                if (convertVideoEntity.getParams() != null) {
                    lineMaker.lineByCustom(listCommand, convertVideoEntity, strOldFilePath, strFileName);
                } else {
                    lineMaker.toMp4ByParam(listCommand, convertVideoEntity, strOldFilePath, strFileName);
                }
            }
        }

        if (listCommand != null) {
            log.info("video command : {}", listCommand);
            try {
                if (new OsInfo().isWindows()) {
                    Process videoProcess = new ProcessBuilder(listCommand).redirectErrorStream(true).start();
                    new PrintStream(videoProcess.getErrorStream()).start();
                    new PrintStream(videoProcess.getInputStream()).start();
                    videoProcess.waitFor();
                } else {
                    log.info("linux开始");
                    StringBuilder strbTest = new StringBuilder();
                    for (String s : listCommand) strbTest.append(s).append(" ");
                    log.info(strbTest.toString());
                    // 执行命令
                    Process p = Runtime.getRuntime().exec(strbTest.toString());
                    // 取得命令结果的输出流
                    InputStream inputStream = p.getInputStream();
                    // 用一个读输出流类去读
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    // 用缓冲器读行
                    BufferedReader br = new BufferedReader(inputStreamReader);
                    String strLine;
                    // 直到读完为止
                    while ((strLine = br.readLine()) != null) {
                        log.info("视频转换:{}", strLine);
                    }
                }

                // 如果进行截图（输出jpg），且配置了“缩略图”参数，则进行缩略图操作
                if (convertVideoEntity.getParams() != null &&
                        convertVideoEntity.getParams().getThumbnail() != null &&
                        "jpg".equalsIgnoreCase(this.strExt)) {
                    Thumbnail thumbnail = convertVideoEntity.getParams().getThumbnail();
                    if (thumbnail.getWidth() > 0 || thumbnail.getHeight() > 0) {
                        // 如果输入了边长，则按边长生成
                        int intImageWidth = 0;
                        int intImageHeight = 0;
                        try (FileInputStream fis = new FileInputStream(new File(strOutputPath + strFileName))) {
                            BufferedImage buffSourceImg = ImageIO.read(fis);
                            BufferedImage buffImg = new BufferedImage(buffSourceImg.getWidth(), buffSourceImg.getHeight(), BufferedImage.TYPE_INT_RGB);
                            // 获取图片的大小
                            intImageWidth = buffImg.getWidth();
                            intImageHeight = buffImg.getHeight();
                        } catch (Exception | Error e) {
                            e.printStackTrace();
                        }

                        if (thumbnail.getWidth() > 0 && thumbnail.getHeight() == 0) {
                            // 如果只输入了宽，则按比例计算高
                            thumbnail.setHeight(thumbnail.getWidth() * intImageHeight / intImageWidth);
                        } else if (thumbnail.getWidth() == 0 && thumbnail.getHeight() > 0) {
                            // 如果只输入了高，则按比例计算宽
                            thumbnail.setWidth(thumbnail.getHeight() * intImageWidth / intImageHeight);
                        }

                        fixedSizeImage(
                                strOutputPath + strFileName,
                                convertVideoEntity.getWriteBack().getOutputPath() + convertVideoEntity.getJpgFileName() + "_thum.jpg",
                                thumbnail.getWidth(),
                                thumbnail.getHeight()
                        );

                    } else if (thumbnail.getScale() >= 0d) {
                        // 如果输入了比例，则按比例生成
                        thumbnail(
                                strOutputPath + strFileName,
                                convertVideoEntity.getWriteBack().getOutputPath() + convertVideoEntity.getJpgFileName() + "_thum.jpg",
                                thumbnail.getScale(),
                                thumbnail.getQuality()
                        );

                    }

                    File fileThum = new File(convertVideoEntity.getWriteBack().getOutputPath() + convertVideoEntity.getJpgFileName() + "_thum.jpg");
                    if (fileThum.exists()) {
                        File fileOut = new File(convertVideoEntity.getWriteBack().getOutputPath() + convertVideoEntity.getJpgFileName() + ".jpg");
                        if (fileOut.exists()) {
                            fileOut.delete();
                        }
                        fileThum.renameTo(fileOut);
                    }

                }

                return true;
            } catch (Exception | Error e) {
                log.error(e);
                return false;
            }

        } else {
            log.error("命令行生成失败！");
            return false;
        }
    }


    /**
     * 图片处理相关方法
     */

    /**
     * 使用给定的图片生成指定大小的图片（原格式）
     *
     * @param strInputFilePath  输入文件的绝对路径和文件名
     * @param strOutputFilePath 输出文件的绝对路径和文件名
     * @param intWidth          输出文件的宽度
     * @param intHeight         输出文件的高度
     */
    public static File fixedSizeImage(String strInputFilePath, String strOutputFilePath,
                                      int intWidth, int intHeight) {
        try {
            Thumbnails.of(strInputFilePath).
                    size(intWidth, intHeight).
                    toFile(strOutputFilePath);
            return new File(strOutputFilePath);
        } catch (IOException e) {
            log.error("原因: " + e.getMessage());
        }
        return null;
    }

    /**
     * 按比例缩放图片
     *
     * @param strInputFilePath  输入文件的绝对路径和文件名
     * @param strOutputFilePath 输出文件的绝对路径和文件名
     * @param dblScale          输出文件的缩放百分比。1为100%,0.8为80%，以此类推。
     * @param dblQuality        输出文件的压缩比（质量）。1为100%,0.8为80%，以此类推。
     */
    public static File thumbnail(String strInputFilePath, String strOutputFilePath,
                                 double dblScale, double dblQuality) {
        try {
            Thumbnails.of(strInputFilePath).
                    //scalingMode(ScalingMode.BICUBIC).
                            scale(dblScale). // 图片缩放80%, 不能和size()一起使用
                    outputQuality(dblQuality). // 图片质量压缩80%
                    toFile(strOutputFilePath);
            return new File(strOutputFilePath);
        } catch (IOException e) {
            log.error("原因: " + e.getMessage());
        }
        return null;
    }

    public static File mp4_2_M3u8(File mp4File, String m3u8FileParentPath, File m3u8FilePath) {
        try {
            if (!FileUtil.exist(mp4File)) {
                return null;
            }
            // 缓存限制
            String cacheKey = SecureUtil.md5(FileUtil.getCanonicalPath(mp4File));
            if (Objects.nonNull(getCacheManager()) && getCacheManager().exists(cacheKey)) {
                log.info("MP4【{}】转m3u8缓存命中", mp4File.getName());
                return m3u8FilePath;
            }

            if (FileUtil.exist(m3u8FilePath)) {
                // 存在删除父级目录
                FileUtil.del(FileUtil.getParent(m3u8FilePath, 1));
            }
            FileUtil.mkParentDirs(m3u8FilePath);

            List<String> listCommand = new ArrayList<>();
            listCommand.add(ConvertVideoConfig.ffmpegFile);

            String mp4FilePath = FileUtil.getCanonicalPath(mp4File);
            listCommand.addAll(new LineMaker().lineByCustomM3u8(mp4FilePath, m3u8FileParentPath));

            Executor taskExecutor = SpringUtil.getBean("taskExecutor");
            taskExecutor.execute(() -> {
                try {
                    log.info("mp4【{}】转m3u8 命令：{}, 开始转换", mp4FilePath, listCommand);
                    Process process = RuntimeUtil.exec(String.join(" ", listCommand));
                    // 取得命令结果的输出流
                    InputStream inputStream = process.getInputStream();
                    // 用一个读输出流类去读
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    // 用缓冲器读行
                    BufferedReader br = new BufferedReader(inputStreamReader);
                    String strLine;
                    // 直到读完为止
                    while ((strLine = br.readLine()) != null) {
                        log.info("mp4转m3u8:{}", strLine);
                    }
                    log.info("mp4 【{}】转 m3u8 完成", mp4FilePath);
                } catch (Exception e) {
                    log.error("mp4 【" + mp4FilePath + "】转m3u8 error", e);
                }
            });
            for (int i = 0; i < ConvertVideoConfig.m3u8ConvertWait; i++) {
                if (FileUtil.exist(m3u8FilePath)){
                    break;
                }
                ThreadUtil.safeSleep(1000);
            }
            return m3u8FilePath;
        } catch (Exception e) {
            log.error("mp4转m3u8 error", e);
        }
        return mp4File;
    }

    private static CacheManager cacheManager;

    @SneakyThrows
    public static CacheManager getCacheManager() {
        if (Objects.isNull(cacheManager)) {
            cacheManager = SpringUtil.getBean(CacheManager.class);
        }
        return cacheManager;
    }
}
