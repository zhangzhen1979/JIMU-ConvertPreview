package com.thinkdifferent.convertpreview.entity;

import com.google.common.reflect.TypeToken;
import com.thinkdifferent.convertpreview.utils.JsonUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 双层PDF文字内容
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/13 15:36
 */
public class Context {
    /**
     * PDF 页码序号，从0开始
     */
    private Integer pageIndex;
    /**
     * PDF 文字内容
     */
    private String text;

    /**
     * 文字坐标、内容
     */
    private List<Word> words;

    public static List<Context> ofList(Object context) {
        if (context == null) {
            return Collections.emptyList();
        }
        Assert.isTrue(context instanceof List, "双层PDF内容格式错误");
        List<Context> contexts = new ArrayList<>();
        JSONArray jaContext = JSONArray.fromObject(context);
        for (Object o : jaContext) {
            JSONObject joContext = (JSONObject) o;
            if (joContext.containsKey("pageIndex") && joContext.containsKey("text")) {
                contexts.add(
                        new Context(joContext.getInt("pageIndex"), joContext.getString("text")).setWords(joContext));
            }
        }
        return contexts;
    }

    public Context() {
    }

    public Context(Integer pageIndex, String text) {
        this.pageIndex = pageIndex;
        this.text = text;
    }

    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (StringUtils.isNotBlank(text)) {
            this.text = text.substring(0, text.endsWith("\n") ? text.length() - 2 : text.length());
        } else {
            this.text = "";
        }
    }

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }

    public Context setWords(JSONObject joContext) {
        if (joContext.containsKey("words")) {
            this.words = JsonUtil.parseObject(joContext.getJSONArray("words").toString(), new TypeToken<List<Word>>() {
            }.getType());
        }
        return this;
    }

    public static class Word {
        private String text;
        private float confidence;
        private Rectangle rect;

        public Word() {
        }

        public Word(String text, float confidence, Rectangle rect) {
            this.text = text;
            this.confidence = confidence;
            this.rect = rect;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }

        public Rectangle getRect() {
            return rect;
        }

        public void setRect(Rectangle rect) {
            this.rect = rect;
        }
    }
}
