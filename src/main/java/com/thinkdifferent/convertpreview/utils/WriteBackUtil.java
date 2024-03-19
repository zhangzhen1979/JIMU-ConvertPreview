package com.thinkdifferent.convertpreview.utils;

import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.entity.ZipParam;
import com.thinkdifferent.convertpreview.entity.writeback.WriteBack;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.lingala.zip4j.exception.ZipException;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Log4j2
public class WriteBackUtil {


    /**
     * 回写
     *
     * @param writeBack 回写对象
     * @param outPutFileType
     * @param fileOut   输出文件
     * @param zipParam
     * @return jo
     */
    public static WriteBackResult writeBack(WriteBack writeBack, String outPutFileType, File fileOut, ZipParam zipParam) throws ZipException {
        if (Objects.isNull(writeBack)) {
            return new WriteBackResult(true);
        }
        return writeBack.writeBack(fileOut);
    }

    /**
     * 回写
     *
     * @param writeBack      回写对象
     * @param outPutFileType 输出文件类型
     * @param fileOut        输出文件, @see ArcZipUtil.zipFile, 做了兼容
     * @param listJpg        jpg list
     * @param zipParam       zip参数
     * @return jo
     */
    @SneakyThrows
    public static WriteBackResult writeBack(WriteBack writeBack, String outPutFileType,
                                            File fileOut, List<String> listJpg, ZipParam zipParam) {
        if (Objects.isNull(writeBack) || !fileOut.getClass().getName().equals("java.io.File")) {
            return new WriteBackResult(true);
        }
        WriteBackResult writeBackResult = writeBack.writeBack(outPutFileType, fileOut, listJpg, zipParam);
        long longPageCount = 0;

        if(listJpg != null && !listJpg.isEmpty()){
            longPageCount = listJpg.size();
            writeBackResult.setPageNum(longPageCount);
        }else{
            // 如果输出zip文件，则经fileOut对象改为pdf文件（结束前删除）。
            if(zipParam != null){
                String strPDF = writeBackResult.getTempFile();
                fileOut = new File(strPDF);
            }else{
                fileOut = new File(writeBackResult.getFile());
//                if("jpg".equalsIgnoreCase(outPutFileType)){
//                    longPageCount = 1;
//                    writeBackResult.setPageNum(longPageCount);
//                }else if("pdf".equalsIgnoreCase(outPutFileType) && fileOut.getName().endsWith(".pdf")){
//                    try (PDDocument doc = PDDocument.load(fileOut, MemoryUsageSetting.setupTempFileOnly())) {
//                        longPageCount = doc.getPages().getCount();
//                        writeBackResult.setPageNum(longPageCount);
//                    }
//
//                }else if("ofd".equalsIgnoreCase(outPutFileType)){
//                    try (OFDReader ofdReader = new OFDReader(Paths.get(fileOut.getCanonicalPath()))) {
//                        longPageCount = ofdReader.getPageList().size();
//                        writeBackResult.setPageNum(longPageCount);
//                    }
//                }
            }
        }

        // 如果输出zip文件，则结束前删除pdf文件。
        if(zipParam != null){
            cn.hutool.core.io.FileUtil.del(fileOut);
        }
        log.info("该文件共有【" + longPageCount + "】页");
        log.info("回写结果：{}", writeBackResult);
        return writeBackResult;
    }

    /**
     * 调用API接口，将文件上传
     *
     * @param strFilePathName 文件路径和文件名
     * @param strUrl          API接口的URL
     * @param mapHeader       Header参数
     * @return 接口返回的JSON
     * @throws Exception
     */
    public static WriteBackResult writeBack2Api(String strFilePathName, String strUrl, Map<String, String> mapHeader)
            throws Exception {
        // 换行符
        final String strNewLine = "\r\n";
        final String strBoundaryPrefix = "--";
        // 定义数据分隔线
        String strBOUNDARY = "========7d4a6d158c9";
        // 服务器的域名
        URL url = new URL(strUrl);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        // 设置为POST情
        httpURLConnection.setRequestMethod("POST");
        // 发送POST请求必须设置如下两行
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setDoInput(true);
        httpURLConnection.setUseCaches(false);
        // 设置请求头参数
        httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
        httpURLConnection.setRequestProperty("Charset", "UTF-8");
        httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + strBOUNDARY);

        @Cleanup OutputStream outputStream = httpURLConnection.getOutputStream();
        @Cleanup DataOutputStream out = new DataOutputStream(outputStream);

        //传递参数
        if (mapHeader != null) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : mapHeader.entrySet()) {
                stringBuilder.append(strBoundaryPrefix)
                        .append(strBOUNDARY)
                        .append(strNewLine)
                        .append("Content-Disposition: form-data; pdfSignName=\"")
                        .append(entry.getKey())
                        .append("\"").append(strNewLine).append(strNewLine)
                        .append(entry.getValue())
                        .append(strNewLine);
            }
            out.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        }

        File file = new File(strFilePathName);
        String sb = strBoundaryPrefix +
                strBOUNDARY +
                strNewLine +
                "Content-Disposition: form-data;pdfSignName=\"file\";filename=\"" + strFilePathName +
                "\"" + strNewLine +
                "Content-Type:application/octet-stream" +
                strNewLine +
                strNewLine;
        out.write(sb.getBytes());

        @Cleanup DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
        byte[] byteBufferOut = new byte[1024];
        int intBytes = 0;
        while ((intBytes = dataInputStream.read(byteBufferOut)) != -1) {
            out.write(byteBufferOut, 0, intBytes);
        }
        out.write(strNewLine.getBytes());

        // 定义最后数据分隔线，即--加上BOUNDARY再加上--。
        byte[] byteEndData = (strNewLine + strBoundaryPrefix + strBOUNDARY + strBoundaryPrefix + strNewLine)
                .getBytes();
        // 写上结尾标识
        out.write(byteEndData);
        out.flush();

        //定义BufferedReader输入流来读取URL的响应
        @Cleanup InputStream inputStream = httpURLConnection.getInputStream();
        @Cleanup InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        @Cleanup BufferedReader reader = new BufferedReader(inputStreamReader);

        String strLine;
        StringBuilder sbLine = new StringBuilder();
        while ((strLine = reader.readLine()) != null) {
            sbLine.append(strLine);
        }

        return new WriteBackResult(true, sbLine.toString());
    }

}
