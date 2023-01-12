package com.thinkdifferent.convertpreview.controller;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.config.RabbitMQConfig;
import com.thinkdifferent.convertpreview.entity.CallBackResult;
import com.thinkdifferent.convertpreview.entity.ConvertBase64Entity;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.service.RabbitMQService;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;

@Log4j2
@Api(tags = "根据传入的JSON参数生成jpg/pdf/ofd文件")
@RestController
@RequestMapping(value = "/api")
public class ConvertController {

    @Autowired
    private ConvertService convertService;

    @Autowired
    private RabbitMQService rabbitMQService;

    /**
     * 接收传入的JSON数据，将源图片文件转换为Jpg、Pdf、Ofd文件；按照传入的设置，将文件回写到指定位置
     *
     * @param jsonInput 输入的JSON对象
     *                  {
     *                  "inputType": "path",
     *                  "inputFile": "D:/cvtest/001.tif",
     *                  "inputHeaders": (未支持)
     *                  {
     *                  "Authorization":"Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
     *                  },
     *                  "outPutFileName": "001-online",
     *                  "outPutFileType": "jpg",
     *                  "writeBackType": "path",
     *                  "writeBack":
     *                  {
     *                  "path":"D:/cvtest/"
     *                  },
     *                  "writeBackHeaders":
     *                  {
     *                  "Authorization":"Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
     *                  },
     *                  "callBackURL": "http://10.11.12.13/callback"
     *                  }
     * @return JSON结果
     */
    @ApiOperation("接收传入的JSON数据，异步将源图片文件转换为jpg、pdf、ofd文件")
    @PostMapping(value = "/convert")
    public Callable<JSONObject> convert2Jpg(@RequestBody JSONObject jsonInput) {
        return (() -> {
            JSONObject jsonReturn = new JSONObject();
            jsonReturn.put("flag", "success");
            try {
                // 检测对象是否存在，不存在会抛出异常
                convertService.checkParams(jsonInput);
                if (!RabbitMQConfig.producer) {
                    convertService.asyncConvert(jsonInput);
                    jsonReturn.put("message", "async convert Success");
                } else {
                    jsonReturn.put("flag", "success");
                    jsonReturn.put("message", "Set Data to MQ Success");

                    rabbitMQService.setData2MQ(jsonInput);
                }
            } catch (Exception e) {
                log.error("error", e);
                jsonReturn.put("flag", "false");
                jsonReturn.put("message", ExceptionUtil.getMessage(e));
            }

            return jsonReturn;
        });
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
    @ApiOperation("接收传入的JSON数据，将源图片文件转换为jpg、pdf、ofd文件，并以Base64字符串输出")
    @PostMapping(value = "/convert2base64")
    public String convert2Base64(@RequestBody JSONObject jsonInput) {

        CallBackResult callBackResult = convertService.convert(jsonInput, "base64");

        if (callBackResult.isFlag()) {
            return callBackResult.getBase64();
        }

        return null;
    }


    /**
     * 接收传入的JSON数据，将源图片文件转换为Jpg、Pdf、Ofd文件，并以Base64字符串输出。
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
     *                  "outPutFileType": "jpg,pdf,ofd"
     *                  }
     * @return JSON结果
     */
    @ApiOperation("接收传入的JSON数据，将源图片文件转换为jpg、pdf、ofd文件，并以Base64字符串输出")
    @RequestMapping(value = "/convert2base64s", method = RequestMethod.POST)
    public ConvertBase64Entity convert2Base64s(@RequestBody JSONObject jsonInput) {

        CallBackResult callBackResult = convertService.convert(jsonInput, "base64");
        ConvertBase64Entity convertBase64Entity = new ConvertBase64Entity(callBackResult);
        if (callBackResult.isFlag()) {
            List<ConvertBase64Entity.SimpleBase64> base64List = new ArrayList<>();

            String strOutPutFileName = jsonInput.getString("outPutFileName");
            String strOutPutFileType = jsonInput.getString("outPutFileType");
            String strTypes = "";

            if (strOutPutFileType.contains("jpg")) {
                strTypes = strTypes + "jpg;";
            }
            if (strOutPutFileType.contains("pdf")) {
                strTypes = strTypes + "pdf;";
            }
            if (strOutPutFileType.contains("ofd")) {
                strTypes = strTypes + "ofd;";
            }

            String strPath = ConvertConfig.outPutPath;
            strPath = SystemUtil.beautifulDir(strPath);
            if (!strPath.endsWith("/")) {
                strPath = strPath + "/";
            }

            if (!"".equals(strTypes)) {
                String[] strType = strTypes.split(";");
                for (String s : strType) {
                    String strFilePathName = strPath + strOutPutFileName + "." + s;
                    File fileOutPut = new File(strFilePathName);
                    if (fileOutPut.exists()) {
                        try {
                            byte[] b = Files.readAllBytes(Paths.get(strFilePathName));
                            base64List.add(new ConvertBase64Entity.SimpleBase64(fileOutPut.getName(),
                                    Base64.getEncoder().encodeToString(b)));
                            // 转换为byte后，PDF文件即可删除
                            FileUtil.del(fileOutPut);
                        } catch (IOException e) {
                            log.error("转换后文件转base64异常", e);
                        }
                    }
                }
                convertBase64Entity.setListBase64(base64List);
            }
        }
        return convertBase64Entity;
    }

}
