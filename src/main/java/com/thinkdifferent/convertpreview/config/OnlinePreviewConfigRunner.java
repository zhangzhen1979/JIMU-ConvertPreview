package com.thinkdifferent.convertpreview.config;

import cn.hutool.extra.spring.SpringUtil;
import com.thinkdifferent.convertpreview.controller.OnlinePreviewController;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author: Ltian
 * @description: 预览启动加载任务
 * @date: 2024/3/20 10:22
 * @version: 1.0
 */
@Component
@Order(1)
@Log4j2
public class OnlinePreviewConfigRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        SpringUtil.getBean(OnlinePreviewController.class).addDefaultConfig();
        log.info("加载默认配置完成");
    }
}
