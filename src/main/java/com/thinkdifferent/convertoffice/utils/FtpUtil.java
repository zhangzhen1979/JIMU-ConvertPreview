package com.thinkdifferent.convertoffice.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.SocketException;

/**
 * ftp工具类
 */

public class FtpUtil {

    private final static Log logger = LogFactory.getLog(FtpUtil.class);
    /** 本地字符编码 */
    private static String LOCAL_CHARSET = "GBK";

    // FTP协议里面，规定文件名编码为iso-8859-1
    private static String SERVER_CHARSET = "ISO-8859-1";

    /**
     * 获取FTPClient对象
     *
     * @param strFtpHost
     *            FTP主机服务器
     *
     * @param intFtpPort
     *            FTP端口 默认为21
     *
     * @param strFtpUserName
     *            FTP登录用户名
     *
     * @param strFtpPassWord
     *            FTP 登录密码
     *
     * @return
     */
    public static FTPClient getFTPClient(String strFtpHost, int intFtpPort, String strFtpUserName, String strFtpPassWord) {
        FTPClient ftpClient = null;
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(strFtpHost, intFtpPort);// 连接FTP服务器
            ftpClient.login(strFtpUserName, strFtpPassWord);// 登陆FTP服务器
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                logger.info("未连接到FTP，用户名或密码错误。");
                ftpClient.disconnect();
            } else {
                logger.info("FTP连接成功。");
            }
        } catch (SocketException e) {
            e.printStackTrace();
            logger.info("FTP的IP地址可能错误，请正确配置。");
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("FTP的端口错误,请正确配置。");
        }
        return ftpClient;
    }

    /**
     * 从FTP服务器下载文件
     *
     * @param strFtpHost FTP IP地址
     *
     * @param intFtpPort FTP端口
     *
     * @param strFtpUserName FTP 用户名
     *
     * @param strFtpPassWord FTP用户名密码
     *
     * @param strFtpPath FTP服务器中文件所在路径 格式： ftptest/aa
     *
     * @param strLocalPath 下载到本地的位置 格式：H:/download
     *
     * @param strFileName 文件名称
     */
    public static void downloadFtpFile(String strFtpHost, int intFtpPort, String strFtpUserName, String strFtpPassWord,
                                       String strFtpPath, String strLocalPath, String strFileName) {

        FTPClient ftpClient = null;

        try {
            ftpClient = getFTPClient(strFtpHost, intFtpPort, strFtpUserName, strFtpPassWord);
            // 设置上传文件的类型为二进制类型
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {// 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）.
                LOCAL_CHARSET = "UTF-8";
            }
            ftpClient.setControlEncoding(LOCAL_CHARSET);
            ftpClient.enterLocalPassiveMode();// 设置被动模式
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);// 设置传输的模式
            // 上传文件
            //对中文文件名进行转码，否则中文名称的文件下载失败
            String strFileNameTemp = new String(strFileName.getBytes(LOCAL_CHARSET), SERVER_CHARSET);
            ftpClient.changeWorkingDirectory(strFtpPath);

            InputStream isRetrieveFile = ftpClient.retrieveFileStream(strFileNameTemp);

            // 第一种方式下载文件(推荐)
			 File localFile = new File(strLocalPath + File.separatorChar + strFileName);
			  OutputStream os = new FileOutputStream(localFile);
			  ftpClient.retrieveFile(strFileName, os); os.close();


            // 第二种方式下载：将输入流转成字节，再生成文件，这种方式方便将字节数组直接返回给前台jsp页面
//            byte[] input2byte = input2byte(isRetrieveFile);
//            byte2File(input2byte, strLocalPath, strFileName);

            if(null != isRetrieveFile){
                isRetrieveFile.close();
            }
        } catch (FileNotFoundException e) {
            logger.error("没有找到" + strFtpPath + "文件");
            e.printStackTrace();
        } catch (SocketException e) {
            logger.error("连接FTP失败.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("文件读取错误。");
            e.printStackTrace();
        } finally {

            if (ftpClient.isConnected()) {
                try {
                    //退出登录
                    ftpClient.logout();
                    //关闭连接
                    ftpClient.disconnect();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Description: 向FTP服务器上传文件
     *
     * @param strFtpHost
     *            FTP服务器hostname
     * @param intFtpPort
     *            FTP服务器端口
     * @param strFtpUserName
     *            FTP登录账号
     * @param strFtpPassWord
     *            FTP登录密码
     * @param strBasePath
     *            FTP服务器基础目录
     * @param strFilePath
     *            FTP服务器文件存放路径。例如分日期存放：/2015/01/01。文件的路径为basePath+filePath
     * @param strFileName
     *            上传到FTP服务器上的文件名
     * @param inputStream
     *            输入流
     * @return 成功返回true，否则返回false
     */
    public static boolean uploadFile(String strFtpHost, int intFtpPort, String strFtpUserName, String strFtpPassWord,
                                     String strBasePath, String strFilePath, String strFileName, InputStream inputStream) {
        boolean blnResult = false;
        FTPClient ftpClient = null;
        try {
            int intReply;
            ftpClient = getFTPClient(strFtpHost, intFtpPort, strFtpUserName, strFtpPassWord);
            intReply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(intReply)) {
                ftpClient.disconnect();
                return blnResult;
            }

            if(!strBasePath.endsWith("/")){
                strBasePath = strBasePath + "/";
            }
            if(strFilePath.startsWith("/")){
                strFilePath = strFilePath.substring(1, strFilePath.length());
            }

            // 切换到上传目录
            if (!ftpClient.changeWorkingDirectory(strBasePath + strFilePath)) {
                // 如果目录不存在创建目录
                String[] strsDirs = strFilePath.split("/");
                String strTempPath = strBasePath;
                for (String strDir : strsDirs) {
                    if (null == strDir || "".equals(strDir))
                        continue;
                    strTempPath += "/" + strDir;
                    if (!ftpClient.changeWorkingDirectory(strTempPath)) {
                        if (!ftpClient.makeDirectory(strTempPath)) {
                            return blnResult;
                        } else {
                            ftpClient.changeWorkingDirectory(strTempPath);
                        }
                    }
                }
            }
            // 设置上传文件的类型为二进制类型
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
                // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）.
                LOCAL_CHARSET = "UTF-8";
            }
            ftpClient.setControlEncoding(LOCAL_CHARSET);
            ftpClient.enterLocalPassiveMode();// 设置被动模式
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);// 设置传输的模式
            // 上传文件
            strFileName = new String(strFileName.getBytes(LOCAL_CHARSET), SERVER_CHARSET);
            if (!ftpClient.storeFile(strFileName, inputStream)) {
                return blnResult;
            }

            if(null != inputStream){
                inputStream.close();
            }

            blnResult = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    //退出登录
                    ftpClient.logout();
                    //关闭连接
                    ftpClient.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return blnResult;
    }

    /**
     * 删除文件
     *
     * @param strFtpHost
     *            FTP服务器地址
     * @param intFtpPort
     *            FTP服务器端口号
     * @param strFtpUserName
     *            FTP登录帐号
     * @param strFtpPassWord
     *            FTP登录密码
     * @param strPathName
     *            FTP服务器保存目录
     * @param strFileName
     *            要删除的文件名称
     * @return
     */
    public static boolean deleteFile(String strFtpHost, int intFtpPort, String strFtpUserName, String strFtpPassWord,
                                     String strPathName, String strFileName) {
        boolean blnFlag = false;
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient = getFTPClient(strFtpHost, intFtpPort, strFtpUserName, strFtpPassWord);
            // 验证FTP服务器是否登录成功
            int intReplyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(intReplyCode)) {
                return blnFlag;
            }
            // 切换FTP目录
            ftpClient.changeWorkingDirectory(strPathName);
            // 设置上传文件的类型为二进制类型
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
                // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）.
                LOCAL_CHARSET = "UTF-8";
            }
            ftpClient.setControlEncoding(LOCAL_CHARSET);
            ftpClient.enterLocalPassiveMode();// 设置被动模式
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);// 设置传输的模式
            //对中文名称进行转码
            strFileName = new String(strFileName.getBytes(LOCAL_CHARSET), SERVER_CHARSET);
            ftpClient.dele(strFileName);
            blnFlag = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    //退出登录
                    ftpClient.logout();
                    //关闭连接
                    ftpClient.disconnect();
                } catch (IOException e) {
                }
            }
        }
        return blnFlag;
    }

    // 将字节数组转换为输入流
    public static final InputStream byte2Input(byte[] byteBuf) {
        return new ByteArrayInputStream(byteBuf);
    }

    // 将输入流转为byte[]
    public static final byte[] input2byte(InputStream inputStream) throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] byteBuff = new byte[100];
        int intRc = 0;
        while ((intRc = inputStream.read(byteBuff, 0, 100)) > 0) {
            swapStream.write(byteBuff, 0, intRc);
        }
        byte[] byteIn2b = swapStream.toByteArray();
        return byteIn2b;
    }

    // 将byte[]转为文件
    public static void byte2File(byte[] byteBuf, String strFilePath, String strFileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File filDir = new File(strFilePath);
            if (!filDir.exists() && filDir.isDirectory()) {
                filDir.mkdirs();
            }
            file = new File(strFilePath + File.separator + strFileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(byteBuf);
        } catch (Exception e) {
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



    public static void main(String[] args) throws FileNotFoundException {
        //ftp服务器IP地址
        String strFtpHost = "10.18.17.190";
        //ftp服务器端口
        int intFtpPort = 21;
        //ftp服务器用户名
        String strFtpUserName = "aa";
        //ftp服务器密码
        String strFtpPassWord = "57ghv2mH";
        //ftp服务器路径
        String strBasePath = "ftpdir/";
//        String strFilePath = "2021/10/";
        String strFilePath = "/";
        //本地路径
        String strLocalPath = "D:/";
        //文件名
        String strFileName = "2.docx";


        //下载
        //将ftp根目录下的文件下载至E盘
        // FtpUtil.downloadFtpFile(strFtpHost, intFtpPort, strFtpUserName, strFtpPassWord, strBasePath + strFilePath, strLocalPath, strFileName);

        //上传
        //将E盘的文件上传至ftp根目录
        FileInputStream inputStream=new FileInputStream(new File("d:/" + strFileName));
        FtpUtil.uploadFile(strFtpHost, intFtpPort, strFtpUserName, strFtpPassWord, strBasePath, strFilePath, strFileName, inputStream);

        //删除
        //删除ftp根目录下的文件
//        FtpUtil.deleteFile(strFtpHost, intFtpPort, strFtpUserName, strFtpPassWord, strBasePath, strFileName);

    }


}