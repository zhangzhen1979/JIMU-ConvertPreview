package com.thinkdifferent.convertpreview.entity;

import com.thinkdifferent.convertpreview.entity.input.*;
import lombok.SneakyThrows;

/**
 * 输入类型
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/13 15:37
 */
public enum InputType {
    PATH(InputPath.class),
    URL(InputUrl.class),
    FTP(InputFtp.class),
    BASE64(InputBase64 .class);

    private final Class<? extends Input> inputClass;

    InputType(Class<? extends Input> inputClass) {
        this.inputClass = inputClass;
    }

    /**
     * 将输入格式转换为标准格式
     *
     * @param strInputPath 输入字符串
     * @param strFileName 文件名
     * @param strExt 文件扩展名
     * @param duplexPrint 单页文件是否补空白页
     * @param blankPageHaveNum 补充的空白页是否添加页码
     * @return input对象
     */
    public Input of(String strInputPath, String strFileName, String strExt,
                    boolean duplexPrint, boolean blankPageHaveNum) {
        try {
            return this.inputClass.newInstance().of(strInputPath, strFileName, strExt,
                    duplexPrint, blankPageHaveNum);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param inputPath 输入的文件路径
     * @param strExt    文件扩展名
     * @return 输入对象
     */
    @SneakyThrows
    public static Input convert(String inputPath, String strFileName, String strExt) {
        for (InputType enums : InputType.values()) {
            Input input = enums.inputClass.newInstance();
            if (input.matchInput(inputPath)) {
                return input.of(inputPath, strFileName, strExt);
            }
        }
        if (inputPath.contains("+")) {
            // 替换+号，浏览器自己转换的
            inputPath = inputPath.replace("+", " ");
            return convert(inputPath, strFileName, strExt);
        }
        throw new IllegalArgumentException("no match input type : " + inputPath);
    }
}
