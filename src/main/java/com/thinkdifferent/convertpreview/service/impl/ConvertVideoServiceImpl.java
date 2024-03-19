package com.thinkdifferent.convertpreview.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.OsInfo;
import com.thinkdifferent.convertpreview.config.ConvertVideoConfig;
import com.thinkdifferent.convertpreview.entity.ConvertVideoEntity;
import com.thinkdifferent.convertpreview.entity.TargetFile;
import com.thinkdifferent.convertpreview.service.ConvertVideoService;
import com.thinkdifferent.convertpreview.utils.ConvertVideoUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频转换实现类
 *
 * @author ltian
 * @version 1.0
 * @date 2023/11/30 14:39
 */
@Log4j2
@Service
public class ConvertVideoServiceImpl implements ConvertVideoService {

    /**
     * 视频文件转换
     *
     * @param convertVideoEntity 视频转换对象
     * @return 转换后文件
     */
    @Override
    public TargetFile convert(ConvertVideoEntity convertVideoEntity) throws IOException {
        TargetFile targetFile = new TargetFile();
        targetFile.setLongPageCount(0);

        // 下载文件
        File fileInput = convertVideoEntity.getInput().getInputFile();
        // 如果文件不需要转换，则直接拷贝
        if("mp3".equalsIgnoreCase(FileUtil.extName(fileInput))){
            File fileOut = new File(convertVideoEntity.getWriteBack().getOutputPath() + convertVideoEntity.getOutPutFileName() + ".mp3");
            FileUtil.copy(fileInput.getPath(), fileOut.getPath(), true);

            targetFile.setTarget(fileOut);

            return targetFile;
        }
        if ("mp4".equalsIgnoreCase(FileUtil.extName(fileInput)) &&
                checkMp4File(fileInput).notConvert()) {
            File fileOut = new File(convertVideoEntity.getWriteBack().getOutputPath() + convertVideoEntity.getOutPutFileName() + ".mp4");
            FileUtil.copy(fileInput.getPath(), fileOut.getPath(), true);

            targetFile.setTarget(mp4_2_m3u8(fileOut));

            return targetFile;
        }

        // 将传入的文件转换为MP4文件，存放到输出路径中
        ConvertVideoUtils convertVideoUtils = new ConvertVideoUtils(fileInput.getCanonicalPath(),
                convertVideoEntity);
        // 转换结果
        boolean blnSuccess = convertVideoUtils.setVoidInfos();

        File fileOut = new File(convertVideoEntity.getWriteBack().getOutputPath() + convertVideoEntity.getOutPutFileName() + "." + convertVideoUtils.getExt());
        // 校验文件有效性
        if (blnSuccess && checkMp4File(fileOut).isNormal()) {
            targetFile.setTarget(mp4_2_m3u8(fileOut));

            return targetFile;
        }
        log.error("视频转换失败");
        return null;
    }

    /**
     * mp4 转 m3u8 预览
     * @param mp4File 转换后的mp4文件
     * @return m3u8 文件
     */
    private static File mp4_2_m3u8(File mp4File){
        if (FileUtil.exist(mp4File) && "mp4".equalsIgnoreCase(FileUtil.extName(mp4File))){
            return ConvertVideoUtils.mp4_2_M3u8(mp4File, FileUtil.getCanonicalPath(mp4File) + "_m3u8"
                    , new File(FileUtil.getCanonicalPath(mp4File) + "_m3u8/index.m3u8"));
        }
        return mp4File;
    }

    /**
     * 检测 mp4 文件的有效性
     *
     * @param mp4File 需要检测的文件
     * @return bln
     */
    private static CheckResult checkMp4File(File mp4File) {
        if (!mp4File.exists()) {
            return CheckResult.NOT_EXIST;
        }
        try {
            List<String> listCommand = new ArrayList<>();
            listCommand.add(ConvertVideoConfig.ffmpegFile);
            listCommand.add("-i");
            listCommand.add(mp4File.getCanonicalPath());
            if (new OsInfo().isWindows()) {
                Process videoProcess = new ProcessBuilder(listCommand).redirectErrorStream(true).start();
                videoProcess.waitFor();

                // 如果返回信息中包含【Video: h264】，则不需要转换，直接将文件复制到output文件夹
                if (StringUtils.isNotBlank(IOUtils.toString(videoProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                    log.error(CheckResult.ERROR.message, IOUtils.toString(videoProcess.getErrorStream(), StandardCharsets.UTF_8));
                    return CheckResult.ERROR;
                } else if (StringUtils.contains(IOUtils.toString(videoProcess.getInputStream(), StandardCharsets.UTF_8), "Video: h264")) {
                    return CheckResult.UN_NEED;
                }
            } else {
                log.info("linux开始");
                StringBuilder strbTest = new StringBuilder();
                for (String s : listCommand) strbTest.append(s).append(" ");
                log.info(strbTest.toString());
                // 执行命令
                Process p = Runtime.getRuntime().exec(strbTest.toString());
                // 取得命令结果的输出流

                // 如果返回信息中包含【Video: h264】，则不需要转换，直接将文件复制到output文件夹
                if (StringUtils.contains(IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8), "Invalid data found when processing input")) {
                    log.error(CheckResult.ERROR.message, IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8));
                    return CheckResult.ERROR;
                } else if (StringUtils.contains(IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8), "Video: h264")) {
                    return CheckResult.UN_NEED;
                }
            }
            return CheckResult.OK;
        } catch (IOException | InterruptedException e) {
            log.error("校验mp4文件【" + mp4File.getName() + "】异常", e);
            return CheckResult.ERROR;
        }
    }

    private enum CheckResult {
        OK("符合转换要求"),
        ERROR("检测出现异常:{}"),
        NOT_EXIST("文件不存在"),
        UN_NEED("不需要进行转换"),
        ;
        String message;

        CheckResult(String message) {

        }

        public boolean isNormal() {
            return StringUtils.equalsAnyIgnoreCase(this.name(), OK.name(), UN_NEED.name());
        }

        public boolean notConvert() {
            return StringUtils.equalsAnyIgnoreCase(this.name(), UN_NEED.name());
        }
    }
}
