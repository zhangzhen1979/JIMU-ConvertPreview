package com.thinkdifferent.convertpreview.entity.params;

import com.thinkdifferent.convertpreview.entity.Thumbnail;

public class Params {

    private int threads;
    private String videoCode;
    private int fps;
    private String resolution;
    private String audioCode;
    private PicMark picMark;
    private TextMark textMark;

    private String time;
    private Thumbnail thumbnail;

    private String custom;


    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public String getVideoCode() {
        return videoCode;
    }

    public void setVideoCode(String videoCode) {
        this.videoCode = videoCode;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAudioCode() {
        return audioCode;
    }

    public void setAudioCode(String audioCode) {
        this.audioCode = audioCode;
    }

    public PicMark getPicMark() {
        return picMark;
    }

    public void setPicMark(PicMark picMark) {
        this.picMark = picMark;
    }

    public TextMark getTextMark() {
        return textMark;
    }

    public void setTextMark(TextMark textMark) {
        this.textMark = textMark;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }
}
