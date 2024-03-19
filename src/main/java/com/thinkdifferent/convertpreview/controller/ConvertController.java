package com.thinkdifferent.convertpreview.controller;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import com.google.common.reflect.TypeToken;
import com.thinkdifferent.convertpreview.cache.CacheService;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.config.RabbitMQConfig;
import com.thinkdifferent.convertpreview.entity.ConvertDocEntity;
import com.thinkdifferent.convertpreview.entity.InputType;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.entity.WriteBackType;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.entity.input.InputBase64;
import com.thinkdifferent.convertpreview.entity.input.InputPath;
import com.thinkdifferent.convertpreview.entity.writeback.WriteBackPath;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.service.RabbitMQService;
import com.thinkdifferent.convertpreview.utils.DocConvertUtil;
import com.thinkdifferent.convertpreview.utils.JsonUtil;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import com.thinkdifferent.convertpreview.utils.WriteBackUtil;
import com.thinkdifferent.convertpreview.vo.PdfCoverVO;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
@RestController
@RequestMapping(value = "/api")
public class ConvertController {

    @Autowired
    private ConvertService convertService;

    @Autowired
    private RabbitMQService rabbitMQService;

    @Resource
    private CacheService cacheService;


    /**
     * 接收传入的JSON数据，将源图片文件转换为Jpg、Pdf、Ofd文件；按照传入的设置，将文件回写到指定位置
     *
     * @param jsonInput 输入的JSON对象
     * @return JSON结果
     */
    @PostMapping(value = "/convert", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JSONObject> convert(@RequestBody JSONObject jsonInput) {

        JSONObject jsonReturn = new JSONObject();
        jsonReturn.put("flag", "success");
        try {
            if (!RabbitMQConfig.producer) {
                convertService.asyncConvert(jsonInput);
                return ResponseEntity.ok(jsonReturn);
            }

            // mq异步
            jsonReturn.put("flag", true);
            jsonReturn.put("message", "Set Data to MQ Success");

            rabbitMQService.setData2MQ(jsonInput);
            return ResponseEntity.ok(jsonReturn);
        } catch (Exception | Error e) {
            log.error("error", e);
            jsonReturn.put("flag", false);
            jsonReturn.put("message", ExceptionUtil.getMessage(e));
            return ResponseEntity.badRequest().body(jsonReturn);
        }
    }

    /**
     * 删除指定的临时文件
     *
     * @param jsonInput 输入的JSON对象
     * @return
     */
    @PostMapping(value = "/deleteTempfile", produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject deleteTempfile(@RequestBody JSONObject jsonInput) {
        JSONObject jsonReturn = new JSONObject();
        jsonReturn.put("flag", "success");
        try {
            // 检测对象是否存在，不存在会抛出异常
            if (!jsonInput.containsKey("file")) {
                jsonReturn.put("flag", "false");
                jsonReturn.put("message", "file参数不存在。请检查传入参数。");
            } else {
                String strFilePathName = SystemUtil.beautifulPath(ConvertDocConfigBase.outPutPath) + jsonInput.getString("file");
                File fileTemp = new File(strFilePathName);
                if (fileTemp.exists()) {
                    fileTemp.delete();
                }

                jsonReturn.put("flag", "success");
                jsonReturn.put("message", "临时文件【" + jsonInput.getString("file") + "】删除成功。");
            }
        } catch (Exception | Error e) {
            log.error("error", e);
            jsonReturn.put("flag", "error");
            jsonReturn.put("message", "临时文件【" + jsonInput.getString("file") + "】删除失败。\n" +
                    ExceptionUtil.getMessage(e));
        }

        return jsonReturn;
    }

    /**
     * 接收传入的JSON数据，将源图片文件转换为Jpg、Pdf、Ofd文件，并在Response中返回流。
     * 本接口只能返回一种格式的转换结果文件
     *
     * @param jsonInput 输入的JSON对象
     *                  {
     *                  "inputType": "path",
     *                  "inputFile": "D:/cvtest/001.tif",
     *                  "inputHeaders":
     *                  {
     *                  "Authorization":"Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
     *                  },
     *                  "outPutFileName": "001-online",
     *                  "outPutFileType": "jpg"
     *                  }
     * @return JSON结果
     */
    @PostMapping(value = "/convert2stream")
    public ResponseEntity<FileSystemResource> convert2stream(@RequestBody JSONObject jsonInput, HttpServletResponse response) {
        try {
            File file = convertService.convert(jsonInput).getTarget();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Content-Disposition", "attachment; filename=" + FileUtil.getName(file));
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("Last-Modified", new Date().toString());
            headers.add("ETag", String.valueOf(System.currentTimeMillis()));
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(new FileSystemResource(file));
        } catch (Exception | Error e) {
            log.error("获取文件转换流异常", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 接收传入的JSON数据，将源图片文件转换为Jpg、Pdf、Ofd文件，并以Base64字符串输出。
     * 本接口只能返回一种格式的转换结果文件
     *
     * @param jsonInput 输入的JSON对象
     *                  {
     *                  "inputType": "path",
     *                  "inputFile": "D:/cvtest/001.tif",
     *                  "inputHeaders":
     *                  {
     *                  "Authorization":"Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
     *                  },
     *                  "outPutFileName": "001-online",
     *                  "outPutFileType": "jpg"
     *                  }
     * @return JSON结果
     */
    @PostMapping(value = "/convert2base64")
    public ResponseEntity<String> convert2Base64(@RequestBody JSONObject jsonInput) {

        try {
            File file = convertService.convert(jsonInput).getTarget();
            return ResponseEntity.ok().body(cn.hutool.core.codec.Base64.encode(file));
        } catch (Exception | Error e) {
            log.error("获取文件转换base64异常", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 接收传入的JSON数据，将保存数据为遮盖后pdf文件
     *
     * @param jsonInput {
     *                  "uuid": "中间保存的ID",
     *                  "inputFile": "base64后完整pdf加密后内容",
     *                  "canvas":[{
     *                  // todo canvas信息另存，重复修改需要
     *                  "page": 1, // 页码
     *                  "data":"base64后canvas内容"
     *                  }]
     *                  ”pageRange“: ”页面范围: 1-5, 1,2,3,4“
     *                  }
     * @return
     */
    @PostMapping(value = "/pdfCover", produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject pdfCover(@RequestBody JSONObject jsonInput) {
        // todo 单独的功能
        JSONObject result = new JSONObject();
        result.put("flag", false);
        WriteBackResult writeBackResult;
        PdfCoverVO pdfCoverVO;
        try {
            String uid = jsonInput.getString("uid");
            String pdfBase64 = jsonInput.getString("inputFile");
            String pageRange = jsonInput.getString("pageRange");
            Assert.hasText(uid, "UID丢失");
            Assert.hasText(pdfBase64, "pdf遮盖内容丢失");
            // 缓存的pdf遮盖信息
            String strPdfCoverVO = cacheService.getStr(uid);
            Assert.hasText(strPdfCoverVO, "获取文件遮盖回写数据失败");
            pdfCoverVO = JsonUtil.parseObject(strPdfCoverVO, new TypeToken<PdfCoverVO>() {
            }.getType());
            Input inputBase64 = new InputBase64().of(pdfBase64, pdfCoverVO.getOutPutFileName(), "pdf");
            File fileInput = inputBase64.getInputFile();

            ConvertDocEntity convertDocEntity = new ConvertDocEntity();
            InputPath inputPath = new InputPath();
            inputPath.setFilePath(fileInput.getAbsolutePath());
            List<Input> listInputs = new ArrayList<>();
            listInputs.add(inputPath);

            // Jpg数组转PDF
            String strFileInputName = SystemUtil.beautifulFilePath(fileInput.getAbsolutePath());
            String strDestFileName = ConvertDocConfigBase.outPutPath + strFileInputName.substring(strFileInputName.lastIndexOf("/") + 1);
            strDestFileName = strDestFileName.substring(0, strDestFileName.lastIndexOf("."));

            convertDocEntity.setInputFiles(listInputs);
            convertDocEntity.setInputType(InputType.PATH);
            convertDocEntity.setOutPutFileName(strDestFileName);
            convertDocEntity.setOutPutFileType("pdf");
            convertDocEntity.setOutPutReadOnly(pdfCoverVO.isOutPutReadOnly());
            convertDocEntity.setPageLimits(pageRange);

            if (StringUtils.isNotBlank(pageRange.trim())) {
                File fileCut = new File(strDestFileName + ".pdf");
                fileCut = DocConvertUtil.cutFile(convertDocEntity, fileCut).getTarget();

                // 更新输入的input对象，后续使用剪裁过的pdf
                inputPath.setFilePath(fileCut.getAbsolutePath());
                listInputs.clear();
                listInputs.add(inputPath);
                convertDocEntity.setInputFiles(listInputs);
            }

            File fileConvert = convertService.convert(JSONObject.fromObject(convertDocEntity)).getTarget();
            String strOutFileName = fileConvert.getName();
            strOutFileName = strOutFileName.substring(0, strOutFileName.lastIndexOf("."));
            fileConvert = FileUtil.rename(fileConvert, strOutFileName, true);
            File fileDest = fileConvert;

            if ("PATH".equalsIgnoreCase(pdfCoverVO.getWriteBackType())) {
                WriteBackPath writeBackPath = (WriteBackPath) WriteBackType.valueOf("PATH").convert(pdfCoverVO.toWriteBackMap());
                File fileDestPath = new File(writeBackPath.getPath());
                if (!fileDestPath.exists()
                        || !fileDestPath.isDirectory()) {
                    fileDestPath.mkdirs();
                }
                FileUtil.copy(fileConvert, fileDestPath, true);
                fileDest = new File(fileDestPath.getAbsolutePath() + "/" + strOutFileName + ".pdf");
            }


            // 回写
            writeBackResult = WriteBackUtil.writeBack(
                    WriteBackType.valueOf(
                            pdfCoverVO.getWriteBackType().toUpperCase()).convert(pdfCoverVO.toWriteBackMap()),
                    "pdf",
                    fileDest,
                    new ArrayList<>(),
                    null);

            // 删除转换的jpg文件和文件夹
            FileUtil.del(strDestFileName);
            // 刪除生成临时文件
            fileConvert.delete();

            // 回调
            if (pdfCoverVO.getCallBackUrl() != null && !"".equals(pdfCoverVO.getCallBackUrl())) {
                convertService.callBack(
                        pdfCoverVO.getCallBackUrl(),
                        pdfCoverVO.getWriteBackHeaders(),
                        writeBackResult,
                        fileDest.getName()
                );
            }
            result.put("flag", true);
            result.put("code", 200);
            result.put("message", "文件保存成功");

        } catch (Exception | Error e) {
            log.error("保存遮盖后pdf异常", e);
            result.put("mssage", "pdf文件保存异常");
        }
        return result;
    }
}
