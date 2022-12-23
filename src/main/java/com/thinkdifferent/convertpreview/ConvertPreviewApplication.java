package com.thinkdifferent.convertpreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.oas.annotations.EnableOpenApi;

import java.util.Properties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableOpenApi
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@EnableConfigurationProperties
@ComponentScan(value = {"com.thinkdifferent.convertpreview"})
public class ConvertPreviewApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ConvertPreviewApplication.class);
        application.setDefaultProperties(defaultProperties());
        application.run(args);
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
        defaultProperties.setProperty("spring.freemarker.check-template-location", "true");
        defaultProperties.setProperty("spring.freemarker.content-type", "text/html");
        defaultProperties.setProperty("spring.freemarker.expose-request-attributes", "true");
        defaultProperties.setProperty("spring.freemarker.expose-session-attributes", "true");
        defaultProperties.setProperty("spring.freemarker.request-context-attribute", "request");
        defaultProperties.setProperty("spring.freemarker.suffix", ".ftl");

        defaultProperties.setProperty("spring.mvc.static-path-pattern", "/static/**");
        return defaultProperties;
    }

}
