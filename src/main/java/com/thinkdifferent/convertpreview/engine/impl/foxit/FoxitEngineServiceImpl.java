package com.thinkdifferent.convertpreview.engine.impl.foxit;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineFoxit;
import com.thinkdifferent.convertpreview.engine.impl.AbstractEngineServiceImpl;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 福昕转换服务引擎
 *
 * @author 张镇
 * @version 3.0
 * @date 2024/1/5 10:42
 */
@Service
@ConditionalOnProperty(name = "convert.engine.foxit.enabled", havingValue = "true")
@Log4j2
public class FoxitEngineServiceImpl extends AbstractEngineServiceImpl {

    /**
     * 引擎实现的pdf文件转换
     *
     * @param inputFile      输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @SneakyThrows
    @Override
    public File doConvertPdf(File inputFile, String strOutputFilePath){
        return convertPdfOrOfd(inputFile, strOutputFilePath);
    }

    /**
     * 引擎实现的ofd文件转换
     *
     * @param inputFile      输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @Override
    public File doConvertOfd(File inputFile, String strOutputFilePath) {
        return convertPdfOrOfd(inputFile, strOutputFilePath);
    }

    /**
     * 福昕转换服务引擎实现的文件转换
     *
     * @param inputFile      输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @SneakyThrows
    private File convertPdfOrOfd(File inputFile, String strOutputFilePath){
        File fileReturn = new File(strOutputFilePath);

        String strInputFile = SystemUtil.beautifulFilePath(inputFile.getAbsolutePath());
        String strFileName = strInputFile.substring(strInputFile.lastIndexOf("/") + 1);
        String strExt = strInputFile.substring(strInputFile.lastIndexOf(".") + 1);
        String strDestExt = strOutputFilePath.substring(strOutputFilePath.lastIndexOf(".") + 1).toLowerCase();

        JSONObject jsonParam = new JSONObject();
        jsonParam.put("url", ConvertDocConfigBase.baseUrl + "/infile/" + strFileName);
        jsonParam.put("srcFormat", strExt);
        jsonParam.put("newFormat", strDestExt);
        jsonParam.put("isSync", true);


        // 判断是否为xls或xlsx格式。如果是，则加入【表格转换参数】，设置为【适配所有列：所有列在一页上】
        JSONObject joParamMap = new JSONObject();
        if("xls".equalsIgnoreCase(strExt) || "xlsx".equalsIgnoreCase(strExt)){
            joParamMap.put("excelOptimize", 2);
        }

        // 是否去掉修订信息
        if(ConvertDocConfigEngineFoxit.foxitAcceptRev){
            joParamMap.put("acceptRev", 1);
        }
        // 是否去掉批注信息
        if(ConvertDocConfigEngineFoxit.foxitShowComments){
            joParamMap.put("showComments", 1);
        }

        jsonParam.put("paramMap", joParamMap);

        cn.hutool.http.HttpResponse htResponse = HttpUtil
                .createPost(ConvertDocConfigEngineFoxit.foxitDomain)
                .form(jsonParam)
                .execute();

        log.debug(htResponse.body());

        if (htResponse.getStatus() == 200) {
            // 取回服务器端的响应结果
            byte[] json = htResponse.bodyBytes();
            String strJSON=new String(json,"UTF-8");

            if(strJSON == null || strJSON.isEmpty()){
                fileReturn = null;
            }else{
                JSONObject jsonObject = JSONObject.fromObject(strJSON);
                int intCode = (Integer)jsonObject.get("code");
                JSONObject joData = jsonObject.getJSONObject("data");
                if(intCode == 0 && joData != null){
                    // 文件转换成功
                    JSONArray jaFileList = joData.getJSONArray("files");
                    if(jaFileList != null && jaFileList.size() > 0){
                        String strFileURL = jaFileList
                                .getJSONObject(0).getJSONArray("paths")
                                .getJSONObject(0).getString("downloadPath");
                        htResponse = HttpUtil
                                .createGet(strFileURL)
                                .execute();

                        log.debug(htResponse.body());

                        FileUtil.writeBytes(htResponse.bodyBytes(), fileReturn);
                    }
                }
            }
        }

        return fileReturn;
    }

    /**
     * 该引擎支持的文件后缀
     *
     * @return list
     */
    @Override
    public List<String> supportFileExt() {
        return Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv");
    }
}
