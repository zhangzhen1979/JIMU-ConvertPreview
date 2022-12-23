package com.thinkdifferent.convertpreview.entity;

import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.entity.input.InputFtp;
import com.thinkdifferent.convertpreview.entity.input.InputPath;
import com.thinkdifferent.convertpreview.entity.input.InputUrl;
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
    FTP(InputFtp.class);
    private final Class<? extends Input> inputClass;

    InputType(Class<? extends Input> inputClass) {
        this.inputClass = inputClass;
    }

    /**
     * 将输入格式转换为标准格式
     *
     * @param strInputPath 输入字符串
     * @return input对象
     */
    public Input of(String strInputPath, String strExt) {
        try {
            return this.inputClass.newInstance().of(strInputPath, strExt);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param inputPath 输入的文件路径
     * @param strExt 文件扩展名
     * @return 输入对象
     */
    @SneakyThrows
    public static Input convert(String inputPath, String strExt) {
        // inputPath = URLDecoder.decode(inputPath, "UTF-8");
        for (InputType enums : InputType.values()) {
            Input input = enums.inputClass.newInstance();
            if (input.matchInput(inputPath)) {
                return input.of(inputPath, strExt);
            }
        }
        throw new IllegalArgumentException("no match input type : " + inputPath);
    }
}
