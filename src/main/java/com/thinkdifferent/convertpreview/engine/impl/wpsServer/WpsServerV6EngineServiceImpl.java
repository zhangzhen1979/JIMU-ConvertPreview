package com.thinkdifferent.convertpreview.engine.impl.wpsServer;

import cn.hutool.http.HttpRequest;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineWpsServer;
import com.thinkdifferent.convertpreview.engine.impl.AbstractEngineServiceImpl;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.thinkdifferent.convertpreview.engine.impl.wpsServer.WpsServerV6ReTryUtil.OPEN_API;

/**
 * wps server v6 引擎
 *
 * @author ltian
 * @version 1.0
 * @date 2024/1/5 10:05
 */
@Log4j2
@Service
@ConditionalOnProperty(name = "convert.engine.wpsPreview.enabled", havingValue = "true")
public class WpsServerV6EngineServiceImpl extends AbstractEngineServiceImpl {
    @Resource
    private WpsServerV6ReTryUtil wpsServerV6ReTryUtil;

    @Value("${convert.engine.wpsPreview.domain:}")
    private String wpsDomain;

    private final String[] types = {"doc", "dot", "wps", "wpt", "docx", "dotx", "docm", "dotm",
            "rtf", "xml", "word_xml", "uof", "uot"};

    /**
     * 引擎实现的pdf文件转换
     *
     * @param inputFile         输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @SneakyThrows
    @Override
    public File doConvertPdf(File inputFile, String strOutputFilePath) {
        return convertPdfOrOfd(inputFile, strOutputFilePath);
    }

    /**
     * 引擎实现的ofd文件转换
     *
     * @param inputFile         输入文件
     * @param strOutputFilePath 转换后文件全路径，带后缀
     * @return 转换后文件
     */
    @Override
    public File doConvertOfd(File inputFile, String strOutputFilePath) {
        return convertPdfOrOfd(inputFile, strOutputFilePath);
    }

    @SneakyThrows
    private File convertPdfOrOfd(File inputFile, String strOutputFilePath) {
        String strInputFile = SystemUtil.beautifulFilePath(inputFile.getAbsolutePath());
        // 如果需要【清稿】，则先调用【内容操作接口】。
        // accept_all_revisions ：指定接受所有修订； delete_all_comments ：删除所有批注； delete_all_ink ：删除所有墨迹
        if (ConvertDocConfigEngineWpsServer.wpsPreviewAcceptAllRevisions ||
                ConvertDocConfigEngineWpsServer.wpsPreviewDeleteAllComments ||
                ConvertDocConfigEngineWpsServer.wpsPreviewAcceptAllRevisions) {
            File fileInputClean = cleanOptions(inputFile);
            // 删除temp文件夹中的有痕迹的文件
            inputFile.delete();
            // 将【清稿】后的文件作为input
            inputFile = fileInputClean;
        }

        String strFileName = strInputFile.substring(strInputFile.lastIndexOf("/") + 1);
        String strDestExt = strOutputFilePath.substring(strOutputFilePath.lastIndexOf(".") + 1).toLowerCase();
        // wps taskId
        String taskId = UUID.randomUUID().toString();
        JSONObject joRequest = new JSONObject();
        joRequest.put("task_id", taskId);
        joRequest.put("doc_filename", strFileName);
        joRequest.put("target_file_format", strDestExt);
        // 原始文件下载地址
        String strOriginalDocUrl = ConvertDocConfigBase.baseUrl + "/infile/" + strFileName;
        joRequest.put("doc_url", strOriginalDocUrl);
        // 判断是否为xls或xlsx格式。如果是，则加入【表格转换参数】，设置为【适配所有列：所有列在一页上】
        String strExt = inputFile.getName().substring(inputFile.getName().lastIndexOf(".") + 1);
        if ("xls".equalsIgnoreCase(strExt) || "xlsx".equalsIgnoreCase(strExt)) {
            JSONObject joEtPageZoom = new JSONObject();
            joEtPageZoom.put("fit_pagetall", 1);
            joRequest.put("et_page_zoom", joEtPageZoom);
        }

        // 调用转换服务
        String strConvertUrl = wpsServerV6ReTryUtil.checkDomain(wpsDomain) + OPEN_API
                + "/api/cps/sync/v1/convert";
        String strResp = HttpRequest.post(strConvertUrl)
                .addHeaders(wpsServerV6ReTryUtil.buildHeaders(strConvertUrl, joRequest.toString(), "POST", new HashMap<>()))
                .body(joRequest.toString())
                .execute()
                .body();
        log.info("同步模式获取WPS结果：{}, reqBody:{}", strResp, joRequest);
        // {"code":200,"msg":"success","data":{"download_id":"c17c9f922c0e4275ad2da9b294922889","route_key":"0"},
        // "request_id":"141606103fdf74ff035b","request_time":1669791085825,"response_time":1669791088797}
        if (isJson(strResp)) {
            JSONObject resp = JSONObject.fromObject(strResp);
            Assert.isTrue(resp.containsKey("code") && 200 == resp.getInt("code"),
                    "WPS V6转换接口出现异常, 请检查。resp= " + resp);
            log.info("调用wps转换接口resp:{}", resp);
            // 重试查询转换结果
            return wpsServerV6ReTryUtil.downloadFile(resp.getJSONObject("data").getString("download_id"),
                    resp.getJSONObject("data").getString("route_key"),
                    strOutputFilePath);
        } else {
            return null;
        }
    }

    /**
     * 如果需要转换前【清稿】，则调用WPS中台的【内容操作接口】，先完成清稿。
     *
     * @param fileInput 输入的文件路径和文件名
     * @return 清稿后的文件对象
     * @throws Exception err
     */
    private File cleanOptions(File fileInput) throws Exception {
        String strFileName = SystemUtil.beautifulFilePath(fileInput.getCanonicalPath());
        String strInputPath = strFileName.substring(0, strFileName.lastIndexOf("/"));
        strFileName = strFileName.substring(strFileName.lastIndexOf("/") + 1);
        String strInputName = strFileName.substring(0, strFileName.lastIndexOf("."));
        String strFileExt = strFileName.substring(strFileName.lastIndexOf(".") + 1).toLowerCase();
        String strDestFilePathName = strInputPath + strInputName + "_clean." + strFileExt;

        // 如果文件格式是WPS支持操作的，则可以去除批注等痕迹。
        if (StringUtils.equalsAnyIgnoreCase(strFileExt, types)) {

            // accept_all_revisions ：指定接受所有修订； delete_all_comments ：删除所有批注； delete_all_ink ：删除所有墨迹
            String strCleanOptions = "";
            if (ConvertDocConfigEngineWpsServer.wpsPreviewAcceptAllRevisions) {
                strCleanOptions = "accept_all_revisions,";
            }
            if (ConvertDocConfigEngineWpsServer.wpsPreviewDeleteAllComments) {
                strCleanOptions = strCleanOptions + "delete_all_comments,";
            }
            if (ConvertDocConfigEngineWpsServer.wpsPreviewDeleteAllInk) {
                strCleanOptions = strCleanOptions + "delete_all_ink,";
            }

            if (!"".equals(strCleanOptions)) {
                // wps taskId
                String taskId = UUID.randomUUID().toString();
                JSONObject joRequest = new JSONObject();
                joRequest.put("task_id", taskId);

                // 原始文件下载地址
                String strOriginalDocUrl = ConvertDocConfigBase.baseUrl + "/infile/" + strFileName;
                joRequest.put("doc_url", strOriginalDocUrl);
                joRequest.put("doc_filename", strFileName);

                JSONObject joStep = new JSONObject();
                joStep.put("operate", "OFFICE_CLEAN");
                JSONArray jaOptions = new JSONArray();
                strCleanOptions = strCleanOptions.substring(0, strCleanOptions.length() - 1);
                jaOptions.add(strCleanOptions);
                JSONObject joOptions = new JSONObject();
                joOptions.put("clean_options", jaOptions);
                joStep.put("args", joOptions);
                JSONArray jaSteps = new JSONArray();
                jaSteps.add(joStep);
                joRequest.put("steps", jaSteps);

                // 调用转换服务
                String strConvertUrl = wpsServerV6ReTryUtil.checkDomain(wpsDomain) + OPEN_API + "/api/cps/sync/v1/content/operate";
                String strResp = HttpRequest.post(strConvertUrl)
                        .addHeaders(wpsServerV6ReTryUtil.buildHeaders(strConvertUrl, joRequest.toString(), "POST", new HashMap<>()))
                        .body(joRequest.toString())
                        .execute()
                        .body();
                log.info("同步模式获取WPS结果：{}, reqBody:{}", strResp, joRequest);
                // {"code":200,"msg":"success","data":{"download_id":"c17c9f922c0e4275ad2da9b294922889","route_key":"0"},
                // "request_id":"141606103fdf74ff035b","request_time":1669791085825,"response_time":1669791088797}
                if (isJson(strResp)) {
                    JSONObject resp = JSONObject.fromObject(strResp);
                    Assert.isTrue(resp.containsKey("code") && 200 == resp.getInt("code"),
                            "WPS V6 内容操作接口出现异常, 请检查。resp= " + resp);
                    log.info("调用wps 内容操作接口resp:{}", resp);
                    // 重试查询转换结果
                    return wpsServerV6ReTryUtil.downloadFile(resp.getJSONObject("data").getString("download_id"),
                            resp.getJSONObject("data").getString("route_key"),
                            strDestFilePathName);
                }
            }
        }
        return null;
    }

    private boolean isJson(String strTxt) {
        try {
            JSONObject.fromObject(strTxt);
            return true;
        } catch (Exception | Error e) {
            return false;
        }
    }


    /**
     * 该引擎支持的文件后缀
     * todo 不全
     *
     * @return list
     */
    @Override
    public List<String> supportFileExt() {
        return Arrays.asList(types);
    }
}
