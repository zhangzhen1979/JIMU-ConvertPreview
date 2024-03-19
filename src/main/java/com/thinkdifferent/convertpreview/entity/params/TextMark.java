package com.thinkdifferent.convertpreview.entity.params;

public class TextMark {

    private String fontFile;
    private String text;
    private int localX;
    private int localY;
    private int fontSize;
    private String fontColor;

    public String getFontFile() {
        return fontFile;
    }

    public void setFontFile(String fontFile) {
        this.fontFile = fontFile;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getLocalX() {
        return localX;
    }

    public void setLocalX(int localX) {
        this.localX = localX;
    }

    public int getLocalY() {
        return localY;
    }

    public void setLocalY(int localY) {
        this.localY = localY;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }
}
