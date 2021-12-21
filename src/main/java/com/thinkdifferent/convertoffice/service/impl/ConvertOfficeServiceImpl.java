package com.thinkdifferent.convertoffice.service.impl;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpConfig;
import cn.hutool.extra.ftp.FtpMode;
import cn.hutool.http.HttpUtil;
import com.thinkdifferent.convertoffice.config.ConvertOfficeConfig;
import com.thinkdifferent.convertoffice.service.ConvertOfficeService;
import com.thinkdifferent.convertoffice.utils.*;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ConvertOfficeServiceImpl implements ConvertOfficeService {

    private static Logger log = LoggerFactory.getLogger(ConvertOfficeServiceImpl.class);

    /**
     * 将传入的JSON对象中记录的文件，转换为PDF/OFD，输出到指定的目录中；回调应用系统接口，将数据写回。
     * @param parameters 输入的参数，JSON格式数据对象
     * @return
     */
    public JSONObject ConvertOffice(Map<String, Object> parameters) {
        JSONObject jsonReturn = new JSONObject();
        jsonReturn.put("flag", "error");
        jsonReturn.put("message", "Convert Office File to Pdf/Ofd Error.");

        /**
         * 输入参数的JSON示例
         *{
         * 	"inputType": "path",
         * 	"inputFile": "D:/1.docx",
         * 	"outPutFileName": "1-online",
         * 	"outPutFileType": "ofd",
         * 	"waterMark":
         *   {
         *     		"waterMarkType":"pic",
         *     		"waterMarkFile":"watermark.png",
         *     		"alpha":"0.5f",
         *     		"LocateX":"150",
         *     		"LocateY":"150",
         *     		"waterMarkWidth":"100",
         *     		"waterMarkHeight":"100"
         *   },
         *   {
         *     		"waterMarkType":"text",
         *     		"waterMarkText":"内部文件",
         *     		"degree":"45",
         *     		"alpha":"0.5f",
         *     		"fontName":"宋体",
         *     		"fontSize":"20",
         *     		"fontColor":"gray"
         *   },
         * 	"writeBackType": "path",
         * 	"writeBack":
         *   {
         *     		"path":"D:/cvtest/"
         *   },
         * 	"writeBackHeaders":
         *   {
         *     		"Authorization":"Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
         *   },
         * 	"callBackURL": "http://10.11.12.13/callback"
         * }
         */

        try {
            // 输入类型（path/url）
            String strInputType = String.valueOf(parameters.get("inputType"));
            // 输入文件（"D:/1.docx"）
            String strInputPath = String.valueOf(parameters.get("inputFile"));
            String strInputFileExt = strInputPath.substring(strInputPath.lastIndexOf(".") +1);

            String strInputPathParam = strInputPath;
            // 默认输入文件存储的临时路径（非path类型时使用）
            String strInPutTempPath = ConvertOfficeConfig.inPutTempPath;
            strInPutTempPath = strInPutTempPath.replaceAll("\\\\", "/");
            if (!strInPutTempPath.endsWith("/")) {
                strInPutTempPath = strInPutTempPath + "/";
            }

            File fileInput = null;

            // 如果输入类型是url，则通过http协议读取文件，写入到默认输出路径中
            if ("url".equalsIgnoreCase(strInputType)) {
                String strInputFileName = strInputPath.substring(strInputPath.lastIndexOf("/") + 1);
                // 检查目标文件夹中是否有重名文件，如果有，先删除。
                fileInput = new File(strInPutTempPath + strInputFileName);
                if (fileInput.exists()) {
                    fileInput.delete();
                }

                // 从指定的URL中将文件读取下载到目标路径
                HttpUtil.downloadFile(strInputPath, strInPutTempPath + strInputFileName);

                strInputPath = strInPutTempPath + strInputFileName;
            } else {
                fileInput = new File(strInputPath);
            }


            // 转换出来的文件名（不包含扩展名）（"1-online"）
            String strOutPutFileName = String.valueOf(parameters.get("outPutFileName"));
            String strOutputType = String.valueOf(parameters.get("outPutFileType"));

            // 文件回写方式（回写路径[path]/回写接口[api]/ftp回写[ftp]）
            String strWriteBackType = "path";

            // 默认输出路径
            String strOutPutPath = ConvertOfficeConfig.outPutPath;
            strOutPutPath = strOutPutPath.replaceAll("\\\\", "/");
            if (!strOutPutPath.endsWith("/")) {
                strOutPutPath = strOutPutPath + "/";
            }

            JSONObject jsonWriteBack = new JSONObject();
            if (parameters.get("writeBackType") != null) {
                strWriteBackType = String.valueOf(parameters.get("writeBackType"));

                // 回写接口或回写路径
                jsonWriteBack = JSONObject.fromObject(parameters.get("writeBack"));
                if ("path".equalsIgnoreCase(strWriteBackType)) {
                    strOutPutPath = jsonWriteBack.getString("path");

                    strOutPutPath = strOutPutPath.replaceAll("\\\\", "/");
                    if (!strOutPutPath.endsWith("/")) {
                        strOutPutPath = strOutPutPath + "/";
                    }
                }
            }


            if (fileInput.exists()) {
                ConvertOfficeUtil convertOfficeUtil = new ConvertOfficeUtil();
                String strPdfFile = strOutPutPath + strOutPutFileName + ".pdf";
                File fileOut = null;

                // 如果输入的文件格式为ofd，则单独处理
                if("ofd".equalsIgnoreCase(strInputFileExt)){
                    if("pdf".equalsIgnoreCase(strOutputType)){
                        fileOut = convertOfficeUtil.convertOfd2Pdf(strInputPath, strPdfFile);
                    }

                }else{
                    // 否则，认为是Office系列文件
                    fileOut = convertOfficeUtil.convertOffice2Pdf(strInputPath, strPdfFile);

                    if (fileOut.exists()) {
                        log.info("文件[" + strInputPathParam + "]转换PDF成功");

                        // 如果设置了需要加水印，则对转换后的pdf加水印
                        if (parameters.containsKey("waterMark")) {
                            String strPdfWaterMark = strOutPutPath + strOutPutFileName + "_wm.pdf";
                            // 获取水印设置
                            JSONObject jsonWaterMark = JSONObject.fromObject(parameters.get("waterMark"));
                            // 水印类型：pic（图片）、text（文字）
                            String strWaterMarkType = jsonWaterMark.getString("waterMarkType");

                            boolean blnSuccess = false;
                            // 如果需要加入图片水印
                            if ("pic".equalsIgnoreCase(strWaterMarkType)) {
                                // 获取水印图片
                                String strWaterMarkFile = System.getProperty("user.dir") + "/watermark/" + jsonWaterMark.getString("waterMarkFile");

                                String strAlpha = jsonWaterMark.getString("alpha");
                                float floatAlpha = 0.5f;
                                if (strAlpha != null && !"".equals(strAlpha)) {
                                    floatAlpha = Float.parseFloat(strAlpha);
                                }
                                Integer intLocateX = jsonWaterMark.getInt("LocateX");
                                Integer intLocateY = jsonWaterMark.getInt("LocateY");
                                Integer intWaterMarkWidth = jsonWaterMark.getInt("waterMarkWidth");
                                Integer intWaterMarkHeight = jsonWaterMark.getInt("waterMarkHeight");

                                blnSuccess = WaterMarkUtil.waterMarkByIcon(strWaterMarkFile, strPdfFile, strPdfWaterMark,
                                        floatAlpha,
                                        intLocateX, intLocateY,
                                        intWaterMarkWidth, intWaterMarkHeight);

                            } else if ("text".equalsIgnoreCase(strWaterMarkType)) {
                                String strWaterMarkText = jsonWaterMark.getString("waterMarkText");
                                Integer intDegree = jsonWaterMark.getInt("degree");
                                String strAlpha = jsonWaterMark.getString("alpha");
                                float floatAlpha = 0.5f;
                                if (strAlpha != null && !"".equals(strAlpha)) {
                                    floatAlpha = Float.parseFloat(strAlpha);
                                }
                                String strFontName = jsonWaterMark.getString("fontName");
                                Integer intFontSize = jsonWaterMark.getInt("fontSize");
                                String strFontColor = jsonWaterMark.getString("fontColor");

                                blnSuccess = WaterMarkUtil.waterMarkByText(strWaterMarkText, strPdfFile, strPdfWaterMark,
                                        floatAlpha, intDegree,
                                        strFontName, intFontSize, strFontColor,
                                        strOutputType);
                            }

                            if (blnSuccess) {
                                fileOut.delete();
                                fileOut = new File(strPdfWaterMark);
                                fileOut.renameTo(new File(strPdfFile));
                            }

                        }


                        if("ofd".equalsIgnoreCase(strOutputType)){
                            File fileOfd = convertOfficeUtil.convertPdf2Ofd(strPdfFile, strOutPutPath + strOutPutFileName + ".ofd");

                            if(fileOfd.exists()){
                                new File(strPdfFile).delete();
                                fileOut = fileOfd;
                            }

                            log.info("文件[" + strInputPathParam + "]转换OFD成功");
                        }

                    } else {
                        jsonReturn.put("flag", "error");
                        jsonReturn.put("message", "Convert Office to " + strOutputType.toUpperCase() + " error.");
                    }

                }


                // 如果是通过url方式获取的源文件，在jpg转换完毕后，作为临时文件，已经无用了，可以删掉。
                if ("url".equalsIgnoreCase(strInputType)) {
                    if (fileInput.exists()) {
                        fileInput.delete();
                    }
                }

                // 如果“回写类型”不是path，则都需要调用工具进行回写（path直接写入了，不用以下这些处理）
                if (!"path".equalsIgnoreCase(strWriteBackType)) {
                    // 回写文件
                    Map mapWriteBackHeaders = new HashMap<>();
                    if (parameters.get("writeBackHeaders") != null) {
                        mapWriteBackHeaders = (Map) parameters.get("writeBackHeaders");
                    }

                    if ("url".equalsIgnoreCase(strWriteBackType)) {
                        String strWriteBackURL = jsonWriteBack.getString("url");

                        jsonReturn = WriteBackUtil.writeBack2Api(fileOut.getCanonicalPath(), strWriteBackURL, mapWriteBackHeaders);

                    } else if ("ftp".equalsIgnoreCase(strWriteBackType)) {
                        // ftp回写
                        boolean blnPassive = jsonWriteBack.getBoolean("passive");
                        String strFtpHost = jsonWriteBack.getString("host");
                        int intFtpPort = jsonWriteBack.getInt("port");
                        String strFtpUserName = jsonWriteBack.getString("username");
                        String strFtpPassWord = jsonWriteBack.getString("password");
                        String strFtpFilePath = jsonWriteBack.getString("filepath");

                        boolean blnFptSuccess = false;
                        FileInputStream in = new FileInputStream(fileOut);

                        Ftp ftp = null;
                        try {
                            if(blnPassive){
                                // 服务器需要代理访问，才能对外访问
                                FtpConfig ftpConfig = new FtpConfig(strFtpHost, intFtpPort,
                                        strFtpUserName, strFtpPassWord,
                                        CharsetUtil.CHARSET_UTF_8);
                                ftp = new Ftp(ftpConfig, FtpMode.Passive);
                            }else{
                                // 服务器不需要代理访问
                                ftp = new Ftp(strFtpHost, intFtpPort,
                                        strFtpUserName, strFtpPassWord);
                            }

                            blnFptSuccess =  ftp.upload(strFtpFilePath, fileOut.getName(), in);

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (ftp != null) {
                                    ftp.close();
                                }

                                if(in != null){
                                    in.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        if (blnFptSuccess) {
                            jsonReturn.put("flag", "success");
                            jsonReturn.put("message", "Upload " + strOutputType.toUpperCase() + " file to FTP success.");
                        } else {
                            jsonReturn.put("flag", "error");
                            jsonReturn.put("message", "Upload " + strOutputType.toUpperCase() + " file to FTP error.");
                        }
                    }

                    String strFlag = jsonReturn.getString("flag");
                    if ("success".equalsIgnoreCase(strFlag)) {
                        if (fileOut.exists()) {
                            fileOut.delete();
                        }
                    }

                    // 回调对方系统提供的CallBack方法。
                    if (parameters.get("callBackURL") != null) {
                        String strCallBackURL = String.valueOf(parameters.get("callBackURL"));

                        Map mapCallBackHeaders = new HashMap<>();
                        if (parameters.get("callBackHeaders") != null) {
                            mapCallBackHeaders = (Map) parameters.get("callBackHeaders");
                        }

                        Map mapParams = new HashMap<>();
                        mapParams.put("file", strOutPutFileName);
                        mapParams.put("flag", strFlag);

                        jsonReturn = callBack(strCallBackURL, mapCallBackHeaders, mapParams);
                    }
                } else {
                    jsonReturn.put("flag", "success");
                    jsonReturn.put("message", "Convert Office to " + strOutputType.toUpperCase() + " success.");
                }

            } else {
                jsonReturn.put("flag", "error");
                jsonReturn.put("message", "Source file not found.");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonReturn;

    }

   /**
     * 回调业务系统提供的接口
     * @param strWriteBackURL 回调接口URL
     * @param mapWriteBackHeaders 请求头参数
     * @param mapParams 参数
     * @return JSON格式的返回结果
     */
    private static JSONObject callBack(String strWriteBackURL, Map<String,String> mapWriteBackHeaders, Map<String, Object> mapParams){
        //发送get请求并接收响应数据
        String strResponse = HttpUtil.createGet(strWriteBackURL).
                addHeaders(mapWriteBackHeaders).form(mapParams)
                .execute().body();

        JSONObject jsonReturn = new JSONObject();
        if(strResponse != null){
            jsonReturn.put("flag", "success");
            jsonReturn.put("message", "Convert Office File Callback Success.\n" +
                    "Message is :\n" +
                    strResponse);
        }

        return jsonReturn;
    }

}
