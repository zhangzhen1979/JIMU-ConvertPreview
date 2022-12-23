package com.thinkdifferent.convertpreview.vo;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.thinkdifferent.convertpreview.consts.ExceptionCodeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * 统一返回对象
 *
 * @author ltian
 * @version 1.0
 * @date 2022/2/14 17:55
 */
@Data
@Accessors
public class MessageBean<T> {
    /**
     * 提示信息
     */
    private String message;

    /**
     * 是否成功
     */
    private boolean blnFlag;

    /**
     * 返回状态码
     */
    private Integer code;

    /**
     * 数据
     */
    private T data;

    /**
     * 无数据返回 消息
     *
     * @return base success bean
     */
    public static MessageBean<String> success() {
        MessageBean<String> messageBean = new MessageBean<>();
        messageBean.setBlnFlag(Boolean.TRUE);
        messageBean.setCode(ExceptionCodeEnum.SUCCESS.getCode());
        messageBean.setMessage(ExceptionCodeEnum.SUCCESS.getMsg());
        return messageBean;
    }

    /**
     * 带有数据返回 消息
     *
     * @param data 要返回的数据
     * @return success bean
     */
    public static <T> MessageBean<T> success(T data) {
        MessageBean<T> messageBean = new MessageBean<>();
        messageBean.setBlnFlag(Boolean.TRUE);
        messageBean.setCode(ExceptionCodeEnum.SUCCESS.getCode());
        messageBean.setMessage(ExceptionCodeEnum.SUCCESS.getMsg());
        messageBean.setData(data);
        return messageBean;
    }

    /**
     * 返回失败 消息
     *
     * @return base bean
     */
    public static <T> MessageBean<T> failure() {
        MessageBean<T> messageBean = new MessageBean<>();
        messageBean.setCode(ExceptionCodeEnum.UN_KNOW.getCode());
        messageBean.setMessage(ExceptionCodeEnum.UN_KNOW.getMsg());
        messageBean.setBlnFlag(Boolean.FALSE);
        return messageBean;
    }

    /**
     * 封装异常信息
     *
     * @param t 异常信息
     * @return bean
     */
    public static <T> MessageBean<T> failure(Throwable t) {
        return failure(ExceptionUtil.getMessage(t));
    }


    /**
     * 封装异常信息
     *
     * @param msg 异常信息
     * @return bean
     */
    public static <T> MessageBean<T> failure(String msg) {
        return failure(ExceptionCodeEnum.UN_KNOW.getCode(), msg);
    }

    /**
     * 返回指定code 消息的失败 消息
     *
     * @param msg  失败提示内容
     * @param code 失败code
     * @return bean
     */
    public static <T> MessageBean<T> failure(Integer code, String msg) {
        MessageBean<T> messageBean = failure();
        if (code != null) {
            messageBean.setCode(code);
        }
        if (Objects.nonNull(msg) && !msg.isEmpty()) {
            messageBean.setMessage(msg);
        }
        return messageBean;
    }

    /**
     * 返回指定code 消息的失败 消息
     *
     * @param msg     失败提示内容
     * @param blnFlag 返回标识
     * @return bean
     */
    public static <T> MessageBean<T> build(boolean blnFlag, String msg) {
        MessageBean<T> messageBean = failure();
        messageBean.setBlnFlag(blnFlag);
        messageBean.setCode(blnFlag ? ExceptionCodeEnum.SUCCESS.getCode() : ExceptionCodeEnum.UN_KNOW.getCode());
        if (Objects.nonNull(msg) && !msg.isEmpty()) {
            messageBean.setMessage(msg);
        }
        return messageBean;
    }
}
