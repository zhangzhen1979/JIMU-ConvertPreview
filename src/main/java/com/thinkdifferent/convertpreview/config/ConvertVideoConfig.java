package com.thinkdifferent.convertpreview.config;

import com.thinkdifferent.convertpreview.utils.SystemUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@RefreshScope
public class ConvertVideoConfig {
    public static List<String> extList;
    @Value(value = "${convert.video.ffmpeg.ext:}")
    public void setExtList(String ext) {
        if (StringUtils.isNotBlank(ext)){
            ConvertVideoConfig.extList = Arrays.asList(ext.split(","));
        }else {
            ConvertVideoConfig.extList = new ArrayList<>();
        }
    }

    public static String ffmpegFile;
    @Value(value = "${convert.video.ffmpeg.file:utils/ffmpeg/ffmpeg}")
    public void setFfmpegFile(String ffmpegFile) {
        ffmpegFile = SystemUtil.getPath(ffmpegFile);

        ConvertVideoConfig.ffmpegFile = ffmpegFile;
        ConvertVideoConfig.ffprobeFile = replaceLast(ffmpegFile, "ffmpeg", "ffprobe");
    }

    public static String ffprobeFile;

    public static int ffmpegThreads;
    @Value(value = "${convert.video.ffmpeg.threads:0}")
    public void setFfmpegThreads(int ffmpegThreads) {
        ConvertVideoConfig.ffmpegThreads = ffmpegThreads;
    }

    public static String ffmpegVideoCode;
    @Value(value = "${convert.video.ffmpeg.videoCode:}")
    public void setFfmpegVideoCode(String ffmpegVideoCode) {
        if(StringUtils.isEmpty(ffmpegVideoCode)){
            ffmpegVideoCode = "libx264";
        }
        ConvertVideoConfig.ffmpegVideoCode = ffmpegVideoCode;
    }

    public static int ffmpegFps;
    @Value(value = "${convert.video.ffmpeg.fps:0}")
    public void setFfmpegFps(int ffmpegFps) {
        ConvertVideoConfig.ffmpegFps = ffmpegFps;
    }

    public static String ffmpegResolution;
    @Value(value = "${convert.video.ffmpeg.resolution:}")
    public void setFfmpegResolution(String ffmpegResolution) {
        ConvertVideoConfig.ffmpegResolution = ffmpegResolution;
    }

    public static String ffmpegAudioCode;
    @Value(value = "${convert.video.ffmpeg.audioCode:}")
    public void setFfmpegAudioCode(String ffmpegAudioCode) {
        if(StringUtils.isEmpty(ffmpegAudioCode)){
            ffmpegAudioCode = "aac";
        }
        ConvertVideoConfig.ffmpegAudioCode = ffmpegAudioCode;
    }

    public static String ffmpegPicFile;
    @Value(value = "${convert.video.ffmpeg.picMark.picFile:}")
    public void setFfmpegPicFile(String ffmpegPicFile) {
        ConvertVideoConfig.ffmpegPicFile = ffmpegPicFile;
    }
    public static String ffmpegPicOverlay;
    @Value("${convert.video.ffmpeg.picMark.overlay:}")
    public void setFfmpegPicOverlay(String ffmpegPicOverlay){
        ConvertVideoConfig.ffmpegPicOverlay = ffmpegPicOverlay;
    }
    public static String ffmpegPicScale;
    @Value("${convert.video.ffmpeg.picMark.scale:}")
    public void setFfmpegPicScale(String ffmpegPicScale){
        ConvertVideoConfig.ffmpegPicScale = ffmpegPicScale;
    }

    public static String ffmpegFontFile;
    @Value(value = "${convert.video.ffmpeg.textMark.fontFile:}")
    public void setFfmpegFontFile(String ffmpegFontFile) {
        ConvertVideoConfig.ffmpegFontFile = ffmpegFontFile;
    }

    public static String ffmpegText;
    @Value(value = "${convert.video.ffmpeg.textMark.text:}")
    public void setFfmpegText(String ffmpegText) {
        ConvertVideoConfig.ffmpegText = ffmpegText;
    }

    public static Integer ffmpegLocalX;
    @Value(value = "${convert.video.ffmpeg.textMark.localX:}")
    public void setFfmpegLocalX(Integer ffmpegLocalX) {
        ConvertVideoConfig.ffmpegLocalX = ffmpegLocalX;
    }

    public static Integer ffmpegLocalY;
    @Value(value = "${convert.video.ffmpeg.textMark.localY:}")
    public void setFfmpegLocalY(Integer ffmpegLocalY) {
        ConvertVideoConfig.ffmpegLocalY = ffmpegLocalY;
    }

    public static Integer ffmpegFontSize;
    @Value(value = "${convert.video.ffmpeg.textMark.fontSize:}")
    public void setFfmpegFontSize(Integer ffmpegFontSize) {
        ConvertVideoConfig.ffmpegFontSize = ffmpegFontSize;
    }

    public static String ffmpegFontColor;
    @Value(value = "${convert.video.ffmpeg.textMark.fontColor:}")
    public void setFfmpegFontColor(String ffmpegFontColor) {
        ConvertVideoConfig.ffmpegFontColor = ffmpegFontColor;
    }

    public static int m3u8DownloadWait;
    @Value(value = "${convert.video.ffmpeg.downloadWait:10}")
    public void setM3u8DownloadWait(int m3u8DownloadWait) {
        ConvertVideoConfig.m3u8DownloadWait = m3u8DownloadWait;
    }

    public static int m3u8ConvertWait;
    @Value(value = "${convert.video.ffmpeg.convertWait:0}")
    public void setM3u8ConvertWait(int m3u8ConvertWait) {
        ConvertVideoConfig.m3u8ConvertWait = m3u8ConvertWait;
    }

    // 替换字符串里最后出现的元素
    public static String replaceLast( String text, String oldText,
                                      String newText ) {
        return text.replaceFirst( "(?s)" + oldText + "(?!.*?" + oldText
                + ")", newText );
    }
}
