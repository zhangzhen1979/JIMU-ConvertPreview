package com.thinkdifferent.convertpreview.config;

import com.thinkdifferent.convertpreview.utils.SystemUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/12/14 10:57
 */
@Component
public class WebConfiguration extends WebMvcConfigurationSupport {

    /**
     * 输入文件的临时路径
     */
    @Value("${convert.path.inPutTempPath:}")
    private String inPutTempPath;
    /**
     * 输出文件的临时路径
     */
    @Value("${convert.path.outPutPath:}")
    private String outPutPath;
    /**
     * 静态文件解析路径
     */
    @Value("${spring.resources.static-locations:}")
    private String staticLocations;
    /**
     * url路径
     */
    @Value("${spring.mvc.static-path-pattern:}")
    private String staticPathPattern;
    /**
     * 解析文件的扩展名
     */
    @Value("${spring.mvc.view.suffix:}")
    private String suffix;


    /**
     * Override this method to add resource handlers for serving static resources.
     *
     * @param registry
     * @see ResourceHandlerRegistry
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        // web解析预览页面
        registry.addResourceHandler("/**").addResourceLocations(ResourceUtils.CLASSPATH_URL_PREFIX + "/templates/");
        registry.addResourceHandler("/**").addResourceLocations(ResourceUtils.CLASSPATH_URL_PREFIX + "/static/");
        // web解析输入路径
        String strInFilePath = SystemUtil.beautifulPath(inPutTempPath);
        if(!strInFilePath.endsWith("intemp/")){
            strInFilePath = strInFilePath + "intemp/";
        }
        registry.addResourceHandler("/infile/**").addResourceLocations("file:" + strInFilePath);
        // web解析输出路径
        String strOutFilePath = SystemUtil.beautifulPath(outPutPath);
        if(!strOutFilePath.endsWith("outtemp/")){
            strOutFilePath = strOutFilePath + "outtemp/";
        }
        registry.addResourceHandler("/outfile/**").addResourceLocations("file:" + strOutFilePath);

        if (StringUtils.isNotEmpty(staticPathPattern) &&
                StringUtils.isNotEmpty(suffix) &&
                StringUtils.isNotEmpty(staticLocations)) {
            registry.addResourceHandler(staticPathPattern + "*" + suffix).addResourceLocations(staticLocations);
        }

    }
}
