package com.thinkdifferent.convertpreview.utils;

import lombok.Cleanup;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/3/25 14:47
 */
public class FileTypeUtil {
    /**
     * 所有的文件类型
     */
    private static final Map<String, String> ALL_FILE_TYPE = new HashMap<>();

    private FileTypeUtil() {
    }

    static {
        initFileType();
    }

    /**
     * 初始化所有文件类型
     */
    private static void initFileType() {
        ALL_FILE_TYPE.put("jpg", "FFD8FF"); //JPEG (jpg)
        ALL_FILE_TYPE.put("png", "89504E47");  //PNG (png)
        ALL_FILE_TYPE.put("gif", "47494638");  //GIF (gif)
        ALL_FILE_TYPE.put("tif", "49492A00");  //TIFF (tif)
        ALL_FILE_TYPE.put("bmp", "424D"); //Windows Bitmap (bmp)
        ALL_FILE_TYPE.put("dwg", "41433130"); //CAD (dwg)
        ALL_FILE_TYPE.put("pdf", "255044462D312E");  //Adobe Acrobat (pdf)
    }

    /**
     * 获取文件类型
     *
     * @param file 文件对象
     * @return 文件类型
     */
    public static String getFileType(File file) throws IOException {
        @Cleanup InputStream is = new FileInputStream(file);
        String strExt = FileTypeUtil.getFileType(is);
        if(strExt == null){
            strExt = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        }
        return strExt;
    }

    /**
     * 输入流获取文件类型
     *
     * @param is 输入流
     * @return 文件类型
     */
    public static String getFileType(InputStream is) throws IOException {
        String filetype = null;
        byte[] b = new byte[50];
        is.read(b);
        filetype = getFileTypeByStream(b);
        return filetype;
    }

    /**
     * 获取文件类型，
     *
     * @return 文件类型； null: 未匹配到对应的文件类型
     */
    private static String getFileTypeByStream(byte[] b) {
        String filetypeHex = String.valueOf(getFileHexString(b));

        return ALL_FILE_TYPE.entrySet().stream()
                .filter(entry -> filetypeHex.toUpperCase().startsWith(entry.getValue()))
                .findFirst().map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 解析读取的is的字符
     */
    private static String getFileHexString(byte[] b) {
        StringBuilder stringBuilder = new StringBuilder();
        if (b == null || b.length <= 0) {
            return null;
        }
        for (byte value : b) {
            int v = value & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
