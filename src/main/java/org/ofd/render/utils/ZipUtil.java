package org.ofd.render.utils;

import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author: Llx
 * @date: 2020/8/12
 * @description:
 **/
public class ZipUtil {


    /**
     * 打包OFD文件
     *
     * @param workDirPath OFD内容工作目录
     * @param zipFileName 文件名称（包含后缀的路径）
     * @throws IOException IO异常
     * @Author: Lianglx
     * @Description: 使用commons-compress的压缩方式重写了原使用zip4j的压缩方式，为了删除zip4j依赖，统一使用commons-compress
     */
    public static void zip(File workDirPath, String zipFileName) throws IOException {
        ZipArchiveOutputStream zaos = null;
        try {
            File zipFile = new File(zipFileName);
            zaos = new ZipArchiveOutputStream(zipFile);
            //Use Zip64 extensions for all entries where they are required
            zaos.setUseZip64(Zip64Mode.AsNeeded);

            //将每个文件用ZipArchiveEntry封装
            //再用ZipArchiveOutputStream写到压缩文件中
            for (File file : allFiles(workDirPath)) {
                if (file != null) {
                    ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(file, file.getAbsolutePath().replace(workDirPath.getAbsolutePath(), "").substring(1));
                    zaos.putArchiveEntry(zipArchiveEntry);
                    InputStream is = null;
                    try {
                        if (file.isDirectory()) continue;
                        is = new BufferedInputStream(new FileInputStream(file));
                        byte[] buffer = new byte[1024 * 4];
                        int len = -1;
                        while ((len = is.read(buffer)) != -1) {
                            //把缓冲区的字节写入到ZipArchiveEntry
                            zaos.write(buffer, 0, len);
                        }
                        //Writes all necessary data for this entry.
                        zaos.closeArchiveEntry();
                    } catch (Exception e) {
                        throw new IOException(e);
                    } finally {
                        if (is != null)
                            is.close();
                    }

                }
            }
            zaos.finish();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (zaos != null) {
                zaos.close();
            }
        }
    }

    private static File[] allFiles(File file) {
        List<File> fileList = new ArrayList<>();

        if (file.exists()) {
            LinkedList<File> list = new LinkedList<>();
            File[] files = file.listFiles();

            for (File file2 : files) {
                if (file2.isDirectory()) {
                    list.add(file2);
                } else {
                    fileList.add(file2);
                }
            }
            File temp_file;
            while (!list.isEmpty()) {
                temp_file = list.removeFirst();
                files = temp_file.listFiles();
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                        list.add(file2);
                    } else {
                        fileList.add(file2);
                    }
                }
            }
        }

        File[] files = new File[fileList.size()];

        return fileList.toArray(files);
    }

    /**
     * 打包OFD文件包二进制数据
     *
     * @param virtualFileMap
     * @return
     * @throws IOException
     * @Author Lianglx
     */
    public static byte[] zip(Map<String, byte[]> virtualFileMap) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(bos);

        for (Map.Entry<String, byte[]> entry : virtualFileMap.entrySet()) {
            zaos.putArchiveEntry(new ZipArchiveEntry(entry.getKey()));
            zaos.write(entry.getValue());
            zaos.closeArchiveEntry();
        }

        zaos.finish();

        return bos.toByteArray();
    }

    /**
     * 打包OFD文件包二进制数据
     *
     * @param virtualFileMap
     * @return
     * @throws IOException
     * @Author Lianglx
     */
    public static void zip(Map<String, byte[]> virtualFileMap,OutputStream output) throws IOException {
        ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(output);

        for (Map.Entry<String, byte[]> entry : virtualFileMap.entrySet()) {
            zaos.putArchiveEntry(new ZipArchiveEntry(entry.getKey()));
            zaos.write(entry.getValue());
            zaos.closeArchiveEntry();
        }

        zaos.finish();
    }
}
