package org.ofd.render.dir;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 资源目录
 *
 * @author 权观宇
 * @since 2020-01-18 02:49:24
 */
public class ResDir implements DirCollect {
    /**
     * 目录内容(文件内容)
     */
    private Map<String, Path> content;
    
    /**
     * 目录内容（文件的二进制内容）
     */
    private Map<String, byte[]> contentOfData;
    
    public ResDir() {
        content = new HashMap<>(5);
        contentOfData = new HashMap<>();
    }

    /**
     * 向目录中加入资源
     *
     * @param res 资源
     * @return this
     */
    public ResDir add(Path res) {
        if (res == null) {
            return this;
        }
        if (Files.notExists(res)) {
            throw new IllegalArgumentException("加入的资源不存在: " + res.toAbsolutePath().toString());
        }
        this.content.put(res.getFileName().toString(), res);
        return this;
    }
    
    /**
     * 向目录中添加资源(资源文件二进制内容)
     * @param name            资源名称
     * @param resData         资源文件二进制内容
     * @return
     */
    public ResDir add(String name, byte[] resData) {
    	if (name == null || name.length() == 0 || resData == null && resData.length == 0) {
    		return this;
    	}
    	
    	contentOfData.put(name, resData);
    	
    	return this;
    }

    /**
     * 获取资源
     *
     * @param name 资源名称（包含后缀）
     * @return 资源路径
     */
    public Path get(String name) {
        return content.get(name);
    }

    /**
     * 创建目录并且复制文件
     *
     * @param baseDir 基础路径
     * @return 创建的目录路径
     * @throws IOException 文件复制异常
     */
    @Override
    public Path collect(String baseDir) throws IOException {
        // 1. 根据基础路径创建目录
        Path path = Paths.get(baseDir, "Res");
        //System.out.println("baseDir:" + baseDir);
        path = Files.createDirectories(path);
        String dir = path.toAbsolutePath().toString();
        if (content == null) {
            return path;
        }
        for (Map.Entry<String, Path> e : content.entrySet()) {
            // 判断被复制文件是否存在
            if (!Files.exists(e.getValue())) {
                continue;
            }
            // 2. 复制文件
            Files.copy(e.getValue(), Paths.get(dir, e.getKey()));
            
        }
        
        //写入二进制文件
        for (Map.Entry<String, byte[]> e : contentOfData.entrySet()) {
        	Files.write(Paths.get(dir, e.getKey()), e.getValue());
        }
        return path;
    }

    @Override
    public Map<String, byte[]> collect(String base, Map<String, byte[]> virtualFileMap) throws IOException {
        // 1. 根据基础路径创建目录
        Path path = Paths.get(base, "Res");
        String dir = path.toString();
        if (content == null) {
            return virtualFileMap;
        }
        for (Map.Entry<String, Path> entry : content.entrySet()) {
//            // 判断被复制文件是否存在
//            if (!Files.exists(e.getValue())) {
//                continue;
//            }
//            // 2. 复制文件
//            Files.copy(e.getValue(), Paths.get(dir, e.getKey()));

            virtualFileMap.put(Paths.get(dir, entry.getKey()).toString(), FileUtils.readFileToByteArray(new File(entry.getKey())));
        }

        //写入二进制文件
        for (Map.Entry<String, byte[]> entry : contentOfData.entrySet()) {
//            Files.write(Paths.get(dir, e.getKey()), e.getValue());
            virtualFileMap.put(Paths.get(dir, entry.getKey()).toString(), entry.getValue());
        }
        return virtualFileMap;
    }
}
