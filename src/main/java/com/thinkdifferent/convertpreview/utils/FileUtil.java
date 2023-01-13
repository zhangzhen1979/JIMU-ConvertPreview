package com.thinkdifferent.convertpreview.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/12/28 9:20
 */
public class FileUtil {
    /**
     * fileTree json 格式，文件格式，返回空字符串
     *
     * @param fileDir 文件目录
     * @return json格式的 fileTree
     */
    public static String fileTree(File fileDir) throws JsonProcessingException {
        if (!fileDir.exists() || fileDir.isFile()) {
            return "";
        }
        List<FileNode> fileNodes = new ArrayList<>();

        addNodes(fileNodes, fileDir);

        return new ObjectMapper().writeValueAsString(fileNodes);
    }

    private static void addNodes(List<FileNode> fileNodeMap, File file) {
        fileNodeMap.add(new FileNode(file.getName(), file.getParentFile().getName(),
                SystemUtil.beautifulFilePath(file.getPath()), file.isDirectory()));
        File[] childFiles;
        if (file.isDirectory() && Objects.nonNull(childFiles = file.listFiles())) {
            for (File childFile : childFiles) {
                addNodes(fileNodeMap, childFile);
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class FileNode {
        private String fileName;
        private String parentFileName;
        private String filePath;
        private boolean directory;
    }


}
