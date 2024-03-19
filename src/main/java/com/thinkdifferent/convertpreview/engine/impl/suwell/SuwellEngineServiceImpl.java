package com.thinkdifferent.convertpreview.engine.impl.suwell;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineSuwell;
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
 * 数科转换服务引擎
 *
 * @author 张镇
 * @version 3.0
 * @date 2024/1/5 10:42
 */
@Service
@ConditionalOnProperty(name = "convert.engine.suwell.enabled", havingValue = "true")
@Log4j2
public class SuwellEngineServiceImpl extends AbstractEngineServiceImpl {


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
     * 数科转换引擎实现的文件转换
     *
     * @param inputFile      输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @SneakyThrows
    private File convertPdfOrOfd(File inputFile, String strOutputFilePath){
        File fileReturn = new File(strOutputFilePath);

        // 先上传文件
        String strInputFile = SystemUtil.beautifulFilePath(inputFile.getAbsolutePath());
        String strFileName = strInputFile.substring(strInputFile.lastIndexOf("/") + 1);
        String strExt = strInputFile.substring(strInputFile.lastIndexOf(".") + 1);
        String strDestExt = strOutputFilePath.substring(strOutputFilePath.lastIndexOf(".") + 1).toLowerCase();

        JSONObject jsonObject = new JSONObject();
        //components.fileLoc | String | 数据url  必须为符合标准的 uri 路径
        jsonObject.put("fileLoc", ConvertDocConfigBase.baseUrl + "/infile/" + strFileName);
        //components.format | String | 数据格式
        jsonObject.put("format", strExt);

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);

        JSONObject joPostBody = new JSONObject();
        joPostBody.put("components", jsonArray);
        //target | String | 结果格式必须为 ofd | pdf| ceb   默认为ofd
        joPostBody.put("target", strDestExt);

        //请求URL，实现文件转换，将转换后的文件存到指定本地路径
        String strPostURL = ConvertDocConfigEngineSuwell.suwellDomain;
        cn.hutool.http.HttpResponse htResponse = HttpUtil.createPost(strPostURL)
                .form(joPostBody)
                .execute();

        log.debug(htResponse.body());

        if(htResponse.getStatus() == 200) {
            FileUtil.writeBytes(htResponse.bodyBytes(), fileReturn);
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
