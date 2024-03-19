package com.thinkdifferent.convertpreview.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/2/14 17:57
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 拦截系统异常
     * 统一返回未知异常。
     *
     * @param ex 拦截所有运行时异常
     * @return 返回响应messageBean
     */
    @ExceptionHandler({Exception.class})
    @ResponseBody
    public ResponseEntity<String> processRuntimeException(Exception ex) {
        log.error(ex.getMessage(), ex);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);

        return builder.body(ex.getMessage());
    }
}
