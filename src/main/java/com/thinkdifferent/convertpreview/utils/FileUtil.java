package com.thinkdifferent.convertpreview.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEnginePOI;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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


    public static void base64ToFile(String base64, String destPath, String fileName) {
        File file = null;
        //创建文件目录
        String filePath = destPath;
        File dir = new File(filePath);
        if (!dir.exists() && !dir.isDirectory()) {
            dir.mkdirs();
        }
        BufferedOutputStream bos = null;
        java.io.FileOutputStream fos = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            file = new File(filePath + "/" + fileName);
            fos = new java.io.FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bytes);
        } catch (Exception | Error e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取文件的ContentType
     *
     * @param fileInput 输入的文件对象
     * @return 返回的文件的ContentType字符串。
     * @throws IOException
     */
    public static String getContentType(File fileInput) {
        return new MimetypesFileTypeMap().getContentType(fileInput);
    }

    /**
     * 是否是文件类型
     *
     * @param file 判断的文件
     * @return bln
     */
    public static boolean isText(File file) {
        String contentType = getContentType(file);
        String strExt = cn.hutool.core.io.FileUtil.extName(file);
        return contentType.contains("text/") ||
                contentType.equalsIgnoreCase("content/unknown") ||
                StringUtils.equalsAnyIgnoreCase(strExt, ConvertDocConfigEnginePOI.txtExt.split(","));
    }


    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private static SecretKeySpec generateKey(String keyStr) {
        byte[] key = keyStr.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(key, "AES");
    }

    public static String encode(String content, String password) throws Exception {
        if (StringUtils.isBlank(password)) {
            return content;
        }

        byte[] iv = password.getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec secretKeySpec = generateKey(password);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);

        byte[] encryptedBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return cn.hutool.core.codec.Base64.encode(encryptedBytes);
    }

}
