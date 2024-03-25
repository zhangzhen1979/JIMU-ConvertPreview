package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/12/27 18:29
 */
@Log4j2
public class ArcZipUtil {
    @Data
    @AllArgsConstructor
    public static class FileNode {
        private String id;
        private String pId;
        private String name;
        private String filePath;
        private String isParent;
    }

    public static class ArcZipFile extends File {
        private final String content;

        public ArcZipFile(@NotNull String pathname, String content) {
            super(pathname);
            this.content = content;
        }

        @Override
        public boolean exists() {
            return StringUtils.isNotBlank(this.content);
        }

        /**
         * 为了兼容预览缓存
         */
        @NotNull
        @Override
        public String getCanonicalPath() throws IOException {
            return "";
        }

        public String getContent() {
            return this.content;
        }
    }

    /**
     * 按照输入的路径，获取压缩包中指定路径的文件、文件夹列表
     *
     * @param zipFile   压缩包文件对象
     * @param pathInZip 压缩包内的路径。默认为空值，即根路径
     * @return
     */
    public static List<String> getFileListByPath(File zipFile, String pathInZip) {
        //1.判断压缩文件是否存在，以及里面的内容是否为空
        if (!zipFile.exists()) {
            log.info(">>>>>>压缩文件【" + zipFile.getName() + "】不存在<<<<<<");
            return null;
        } else if (0 == zipFile.length()) {
            log.info(">>>>>>压缩文件【" + zipFile.getName() + "】大小为0不需要解压<<<<<<");
            return null;
        } else {
            //7zip 获取文件列表
            RandomAccessFile randomAccessFile = null;
            IInArchive inArchive = null;
            try {
                randomAccessFile = new RandomAccessFile(zipFile, "r");
                RandomAccessFileInStream t = new RandomAccessFileInStream(randomAccessFile);
                inArchive = SevenZip.openInArchive(null, t);
//            inArchive = SevenZip.openInArchive(ArchiveFormat.ZIP, t);
                ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();

                List<String> listFilePath = new ArrayList<>();

                int intPathP = 0;
                if (!StringUtils.isEmpty(pathInZip)) {
                    pathInZip = SystemUtil.beautifulPath(pathInZip);
                    intPathP = pathInZip.split("/").length;
                }

                for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                    String strPathFile = SystemUtil.beautifulFilePath(item.getPath());
                    int intZipP = strPathFile.split("/").length - 1;

                    if (item.isFolder()) {
                        strPathFile = strPathFile + "/";
                    }
                    // 获取根目录下的文件（判断是否是文件夹，如果是文件夹，结尾加【/】
                    if (StringUtils.isEmpty(pathInZip) &&
                            (intZipP == 0 || intZipP == -1)) {
                        listFilePath.add(strPathFile);
                        continue;
                    } else if (!StringUtils.isEmpty(pathInZip) &&
                            strPathFile.startsWith(pathInZip) &&
                            intPathP == intZipP) {
                        // 获取指定文件夹下的文件
                        listFilePath.add(strPathFile);
                        continue;
                    }
                }
                return listFilePath;
            } catch (Exception | Error e) {
                log.error("压缩包解压失败: " + e);
                return null;
            } finally {
                if (inArchive != null) {
                    try {
                        inArchive.close();
                    } catch (SevenZipException ignored) {
                    }
                }
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    /**
     * 在压缩包中解压一个指定的文件
     *
     * @param zipFile       压缩文件File对象
     * @param fileInZip     文件在压缩包中的路径和文件名（可从压缩包的header信息中获取）
     * @param targetFileDir 解压后文件存放的目标文件夹
     * @param password      压缩包密码。
     * @return 解压后的文件对象（File）
     */
    public static File unzipOneFile(File zipFile, String fileInZip, String targetFileDir, String password) {
        fileInZip = SystemUtil.beautifulFilePath(fileInZip);

        RandomAccessFile randomAccessFile = null;
        IInArchive inArchive = null;
        RandomAccessFileOutStream rafo = null;
        try {
            // 判断目标目录是否存在，不存在则创建
            File fileTargetDir = new File(targetFileDir);
            if (!fileTargetDir.exists()) {
                fileTargetDir.mkdirs();
            }

            randomAccessFile = new RandomAccessFile(zipFile, "r");
            RandomAccessFileInStream t = new RandomAccessFileInStream(randomAccessFile);
            inArchive = SevenZip.openInArchive(null, t);
            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();

            for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                if (!item.isFolder()) {
                    if (fileInZip.indexOf("/") != fileInZip.length() - 1) {
                        String strFilePath = SystemUtil.beautifulFilePath(item.getPath());
                        String strTargetFilePath = targetFileDir + strFilePath.substring(strFilePath.lastIndexOf("/") + 1);
                        File fileTarget = new File(strTargetFilePath);

                        if (strFilePath.equals(fileInZip)) {
                            rafo = new RandomAccessFileOutStream(new RandomAccessFile(strTargetFilePath , "rw"));
                            ExtractOperationResult result = item.extractSlow(rafo, password);

                            if (result != ExtractOperationResult.OK) {
                                log.error("压缩包提取文件失败: {}, file:{}", result, item.getPath()  );
                                break;
                            }

                            if (fileTarget.exists()) {
                                return fileTarget;
                            }

                        }
                    }
                }

            }
        } catch (Exception | Error e) {
            log.error("压缩包解压失败: " + e);
        } finally {
            IoUtil.close(inArchive);
            IoUtil.close(randomAccessFile);
            try {
                if (rafo != null){
                    rafo.close();
                }
            }catch (Exception e){

            }
        }
        return null;
    }

    public static ArcZipFile treeFile(File zipFile, String pathInZip) {
        return new ArcZipFile(FileUtil.getCanonicalPath(zipFile), fileTree(zipFile, pathInZip));
    }

    public static String readZipFree(File file) {
        Assert.isTrue(file instanceof ArcZipFile, "zipfile 格式错误");
        return ((ArcZipFile) file).getContent();
    }

    public static String fileTree(File zipFile, String pathInZip, String parentPref) {
        List<FileNode> fileNodeList = new ArrayList<>();

        //1.判断压缩文件是否存在，以及里面的内容是否为空
        if (!zipFile.exists()) {
            log.info(">>>>>>压缩文件【" + zipFile.getName() + "】不存在<<<<<<");
            return "{}";
        } else if (0 == zipFile.length()) {
            log.info(">>>>>>压缩文件【" + zipFile.getName() + "】大小为0不需要解压<<<<<<");
            return "{}";
        } else {
            //7zip 获取文件列表
            RandomAccessFile randomAccessFile = null;
            IInArchive inArchive = null;
            try {
                randomAccessFile = new RandomAccessFile(zipFile, "r");
                RandomAccessFileInStream t = new RandomAccessFileInStream(randomAccessFile);
                inArchive = SevenZip.openInArchive(null, t);
                ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();


                int intPathP = 0;
                if (!StringUtils.isEmpty(pathInZip)) {
                    pathInZip = SystemUtil.beautifulPath(pathInZip);
                    intPathP = pathInZip.split("/").length;
                }
                // 记录路径及ID
                Map<String, String> mapFileIndex = new HashMap<>();
                // 所有文件，已解压层级
                int i = 0;
                for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                    String strPathFile = SystemUtil.beautifulFilePath(item.getPath());

                    int intZipP = strPathFile.split("/").length - 1;
                    if (item.isFolder()) {
                        strPathFile = strPathFile + "/";
                    }

                    if (
                        // 获取根目录下的文件（判断是否是文件夹，如果是文件夹，结尾加【/】
                            (StringUtils.isEmpty(pathInZip) && (intZipP == 0 || intZipP == -1)) ||
                                    // 获取指定文件夹下的文件
                                    (!StringUtils.isEmpty(pathInZip) && strPathFile.startsWith(pathInZip) && intPathP == intZipP)
                    ) {
                        mapFileIndex.put(strPathFile, String.valueOf(++i));
                        String parentPath = StringUtils.substring(strPathFile, 0, strPathFile.lastIndexOf("/"));
                        fileNodeList.add(new FileNode(
                                parentPref + i+"_", mapFileIndex.getOrDefault(parentPath, parentPref), FileUtil.getName(strPathFile),
                                strPathFile, String.valueOf(item.isFolder())
                        ));
                        continue;
                    }
                }

//                int i = 0;
//                // 记录路径及ID
//                Map<String, Integer> mapFileIndex = new HashMap<>();
//                String zipFilePath = FileUtil.getCanonicalPath(zipFile);
//                // 所有文件，已解压层级
//                for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
//                    String strPathFile = SystemUtil.beautifulFilePath(item.getPath());
//
//                    mapFileIndex.put(strPathFile, ++i);
//                    String parentPath = StringUtils.substring(strPathFile, 0, strPathFile.lastIndexOf("/"));
//                    fileNodeList.add(new FileNode(
//                            i,mapFileIndex.getOrDefault(parentPath, -1).toString() , FileUtil.getName(strPathFile),
//                            strPathFile, String.valueOf(item.isFolder())
//                    ));
//                }

                return new ObjectMapper().writeValueAsString(fileNodeList);
            } catch (Exception | Error e) {
                log.error("压缩包解压失败: " + e);
                return "{}";
            } finally {
                if (inArchive != null) {
                    try {
                        inArchive.close();
                    } catch (SevenZipException ignored) {
                    }
                }
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    /**
     * 获取压缩包内文件列表
     */
    public static String fileTree(File zipFile, String pathInZip) {
        return fileTree(zipFile, pathInZip, "");
    }


    /**
     * 7z解压压缩包
     *
     * @param zipFile       7z文件
     * @param targetFileDir 目标文件夹
     * @param password      解压密码
     * @return 解压后文件夹
     * @throws IOException err
     */
    public static File unzip(File zipFile, String targetFileDir, String password) throws IOException {
        if (Objects.isNull(password)) {
            password = "";
        }
        // 保存目录带压缩包文件名
        if (!(targetFileDir.endsWith(zipFile.getName()) || targetFileDir.endsWith(zipFile.getName() + "/"))) {
            targetFileDir = Paths.get(targetFileDir, zipFile.getName()).toFile().getCanonicalPath();
        }
        //1.判断压缩文件是否存在，以及里面的内容是否为空
        if (!zipFile.exists()) {
            log.info(">>>>>>压缩文件【" + zipFile.getName() + "】不存在<<<<<<");
            return null;
        } else if (0 == zipFile.length()) {
            log.info(">>>>>>压缩文件【" + zipFile.getName() + "】大小为0不需要解压<<<<<<");
            return null;
        } else if (FileUtil.exist(targetFileDir) && Objects.requireNonNull(FileUtil.file(targetFileDir).listFiles()).length > 0) {
            // 文件已解压，直接返回
            return new File(targetFileDir);
        } else {
            //7zip 解压文件
            return unzipFile_(zipFile, targetFileDir, password);
        }
    }

    private static File unzipFile_(File zipFile, String targetFileDir, String password) {
        RandomAccessFile randomAccessFile = null;
        IInArchive inArchive = null;
        try {
            // 判断目标目录是否存在，不存在则创建
            File newdir = new File(targetFileDir);
            if (!newdir.exists()) {
                newdir.mkdirs();
            }
            randomAccessFile = new RandomAccessFile(zipFile, "r");
            RandomAccessFileInStream t = new RandomAccessFileInStream(randomAccessFile);
            inArchive = SevenZip.openInArchive(null, t);
//            inArchive = SevenZip.openInArchive(ArchiveFormat.ZIP, t);
            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
            for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                String strItemPath = SystemUtil.beautifulFilePath(item.getPath());
                if (!item.isFolder()) {
                    ExtractOperationResult result;
                    result = item.extractSlow(data -> {
                        //写入指定文件
                        FileOutputStream fos1 = null;
                        try {
                            if (strItemPath.indexOf("/") > 0) {
                                String path = targetFileDir + "/" + strItemPath.substring(0, strItemPath.lastIndexOf("/"));
                                File folderExisting = new File(path);
                                if (!folderExisting.exists())
                                    new File(path).mkdirs();
                            }

                            fos1 = new FileOutputStream(targetFileDir + "/" + strItemPath, true);
                            log.debug(">>>>>>保存文件至：" + targetFileDir + "/" + strItemPath);
                            fos1.write(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            IOUtils.closeQuietly(fos1);
                        }
                        return data.length; // Return amount of consumed data
                    }, password);

                    if (result != ExtractOperationResult.OK) {
                        log.error("压缩包提取文件失败: {}, file:{}", result, item.getPath());
                    }
                }
            }
            return newdir;
        } catch (Exception | Error e) {
            log.error("压缩包解压失败: " + e);
            return null;
        } finally {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (SevenZipException ignored) {
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException ignored) {
                }
            }
        }
    }


}
