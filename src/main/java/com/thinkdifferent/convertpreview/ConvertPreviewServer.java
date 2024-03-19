package com.thinkdifferent.convertpreview;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.service.ConvertService;
import net.sf.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.util.Properties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@EnableConfigurationProperties
public class ConvertPreviewServer {

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new ErrorUncaughtExceptionHandler());
        SpringApplication application = new SpringApplication(ConvertPreviewServer.class);
        application.setDefaultProperties(defaultProperties());
        application.run(args);

        // 判断设置的默认输入、输出路径是否存在，不存在则创建
        try {
            File fileInPutDir = new File(ConvertDocConfigBase.inPutTempPath);
            if (!fileInPutDir.exists()
                    || !fileInPutDir.isDirectory()) {
                fileInPutDir.mkdirs();
            }
            File fileOutPutDir = new File(ConvertDocConfigBase.outPutPath);
            if (!fileOutPutDir.exists()
                    || !fileOutPutDir.isDirectory()) {
                fileOutPutDir.mkdirs();
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
        // 重启后判断inputTemp/inputJson/目录下是否有*.json文件，重启该部分任务
        FileUtil.loopFiles(FileUtil.file(ConvertDocConfigBase.inPutTempPath, "inputJson"), pathname ->
                pathname.exists() && pathname.isFile() && pathname.getName().endsWith(".json"))
                .forEach(file -> {
                    String jsonData = FileUtil.readUtf8String(file);
                    JSONObject jsonInput = JSONObject.fromObject(jsonData);
                    SpringUtil.getBean(ConvertService.class).convert(jsonInput);
                });
    }

    /**
     * 默认的配置参数
     *
     * @return 默认配置
     */
    private static Properties defaultProperties() {
        Properties defaultProperties = new Properties();
        // SpringBoot Admin
        // 不启用 rabbit 监控， 兼容问题， cloud 就可以， 不启用无MQ就不监控了
        defaultProperties.setProperty("management.health.rabbit.enabled", "false");
        // yml 中需要带单引号， 其他不需要
        defaultProperties.setProperty("management.endpoints.web.exposure.include", "*");
        defaultProperties.setProperty("spring.boot.admin.client.url", "http://localhost:9999");
        defaultProperties.setProperty("logging.level.de.codecentric.boot.admin.client", "error");
        // freemaker 配置
        defaultProperties.setProperty("spring.freemarker.template-loader-path", "classpath:/templates/");
        defaultProperties.setProperty("spring.freemarker.cache", "false");
        defaultProperties.setProperty("spring.freemarker.charset", "UTF-8");
        defaultProperties.setProperty("spring.freemarker.check-template-pdfSignLocation", "true");
        defaultProperties.setProperty("spring.freemarker.content-type", "text/html");
        defaultProperties.setProperty("spring.freemarker.expose-request-attributes", "true");
        defaultProperties.setProperty("spring.freemarker.expose-session-attributes", "true");
        defaultProperties.setProperty("spring.freemarker.request-context-attribute", "request");
        defaultProperties.setProperty("spring.freemarker.suffix", ".ftl");

        defaultProperties.setProperty("spring.mvc.static-path-pattern", "/static/**");
        return defaultProperties;
    }

    static class ErrorUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            System.err.println(t.getName() + "未捕获的异常");
            e.printStackTrace();
        }
    }
}
