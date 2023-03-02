package com.thinkdifferent.convertpreview.consts;

/**
 * 错误异常信息提示
 * @author ltian
 * @version 1.0
 * @date 2022/2/14 18:01
 */
public enum ExceptionCodeEnum {

    /**
     * 成功状态码，统一维护在这里
     */
    SUCCESS(2000, "成功"),

    /**
     * 3* 重定向 需要进一步操作完成请求
     */
    REDIRECT(3002, "默认重定向"),
    NOT_LOGIN(3003, "用户未登录重定向"),

    /**
     * 4* 客户端错误
     */
    PARAMS_ERROR(4001, "参数解析错误"),
    PERMISSION_ERROR(4002, "权限不足错误"),

    /**
     * 5* 服务器错误
     */
    UN_KNOW(5001, "系统开小差了，请稍后重试"),
    THIRD_API_ERROR(5002, "第三方api 调用错误"),
    REDIS_ERROR(5003, "缓存错误"),
    REDIS_PARAMS_ERROR(5004, "缓存参数错误"),
    DATA_NOT_FIND_ERROR(5010, "数据未找到"),


    /**
     * 9* 业务扩展自定义状态码具体规范由业务自行规范
     */
    PROPERTIES_ERROR(9000, "DB连接异常"),
    DB_CONNECT_ERROR(9001, "数据库连接异常"),
    FILE_ERROR(9003, "文件处理异常");

    ;
    private final Integer code;

    private final String msg;

    ExceptionCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
