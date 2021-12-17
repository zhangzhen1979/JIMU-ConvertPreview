package org.ofd.render.dir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 页面容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:34:34
 */
public class TemplatesDir implements DirCollect {

    /**
     * 容器
     */
    private List<TemplateDir> container;

    public TemplatesDir() {
        this.container = new ArrayList<>(5);
    }

    /**
     * 增加页面
     *
     * @param template 页面容器
     * @return this
     */
    public TemplatesDir add(TemplateDir template) {
        if (container == null) {
            container = new ArrayList<>(5);
        }
        this.container.add(template);
        return this;
    }

    /**
     * 获取指定页面容器
     *
     * @param index 页码（从1开始）
     * @return this
     */
    public TemplateDir get(Integer index) {
        if (container == null) {
            return null;
        }
        for (TemplateDir template : container) {
            if (template.getIndex().equals(index)) {
                return template;
            }
        }
        return null;
    }

    /**
     * 创建目录并复制文件
     *
     * @param base 基础路径
     * @return 创建的目录路径
     * @throws IOException IO异常
     */
    @Override
    public Path collect(String base) throws IOException {
        if (container == null || container.isEmpty()) {
            throw new IllegalArgumentException("缺少页面");
        }
        Path path = Paths.get(base, "Tpls");
        path = Files.createDirectories(path);
        String dir = path.toAbsolutePath().toString();
        for (TemplateDir p :container) {
            p.collect(dir);
        }
        return path;
    }

    @Override
    public Map<String, byte[]> collect(String base, Map<String, byte[]> virtualFileMap) throws IOException {
        if (container == null || container.isEmpty()) {
            throw new IllegalArgumentException("缺少页面");
        }
        Path path = Paths.get(base, "Tpls");
        String dir = path.toString();
        for (TemplateDir p : container) {
            p.collect(dir, virtualFileMap);
        }
        return virtualFileMap;
    }
}
