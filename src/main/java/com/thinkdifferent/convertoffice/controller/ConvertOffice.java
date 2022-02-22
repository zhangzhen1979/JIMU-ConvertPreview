package com.thinkdifferent.convertoffice.controller;

import com.thinkdifferent.convertoffice.config.ConvertOfficeConfig;
import com.thinkdifferent.convertoffice.config.RabbitMQConfig;
import com.thinkdifferent.convertoffice.service.ConvertOfficeService;
import com.thinkdifferent.convertoffice.service.RabbitMQService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

@Api(tags="根据传入的JSON参数将Office文件转换为Pdf文件")
@RestController
@RequestMapping(value = "/api")
public class ConvertOffice {

    @Autowired
    private ConvertOfficeService convertOfficeService;

    @Autowired
    private RabbitMQService rabbitMQService;

    /**
     * 接收传入的JSON数据，将源Office文件转换为Pdf文件；按照传入的设置，将文件回写到指定位置
     * @param jsonInput 输入的JSON对象
     *{
     * 	"inputType": "path",
     * 	"inputFile": "D:/1.docx",
     * 	"inputHeaders":
     *  {
     *     		"Authorization":"Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
     *   },
     * 	"outPutFileName": "1-online",
     * 	 "outPutFileType": "ofd",
     * 	"writeBackType": "path",
     * 	"writeBack":
     *   {
     *     		"path":"D:/"
     *   },
     * 	"writeBackHeaders":
     *   {
     *     		"Authorization":"Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
     *   },
     * 	"callBackURL": "http://10.11.12.13/callback"
     * }
     * @return JSON结果
     */
    @ApiOperation("接收传入的JSON数据，将源Office文件转换为Pdf/Ofd文件；按照传入的设置，将文件回写到指定位置")
    @RequestMapping(value = "/convert", method = RequestMethod.POST)
    public Map<String, String> convert2Jpg(@RequestBody JSONObject jsonInput) {
        JSONObject jsonReturn = new JSONObject();

        if(!RabbitMQConfig.producer){
            jsonReturn = convertOfficeService.ConvertOffice(jsonInput);
        }else{
            jsonReturn.put("flag", "success" );
            jsonReturn.put("message", "Set Data to MQ Success" );

            rabbitMQService.setData2MQ(jsonInput);
        }

        return jsonReturn;
    }

    /**
     * 接收传入的JSON数据，将源Office文件转换为Pdf/Ofd文件，并以Base64字符串输出。
     * 本接口只能返回一个文件的转换结果的字符串。
     * @param jsonInput 输入的JSON对象
     *{
     * 	"inputType": "path",
     * 	"inputFile": "D:/1.docx",
     * 	"inputHeaders":
     *  {
     *     		"Authorization":"Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
     *   },
     *   "outPutFileName": "1-online",
     *   "outPutFileType": "ofd"
     * }
     * @return String结果
     */
    @ApiOperation("接收传入的JSON数据，将源Office文件转换为Pdf文件，并以Base64字符串输出。本接口只能返回一个文件的转换结果的字符串。")
    @RequestMapping(value = "/convert2base64", method = RequestMethod.POST)
    public String convert2Base64(@RequestBody JSONObject jsonInput) {

        JSONObject jsonReturn = convertOfficeService.ConvertOffice(jsonInput);

        if("success".equalsIgnoreCase(jsonReturn.getString("flag"))){
            String strPath = ConvertOfficeConfig.outPutPath;
            strPath = strPath.replaceAll("\\\\", "/");
            if(!strPath.endsWith("/")){
                strPath = strPath + "/";
            }

            String strOutPutFileName = jsonInput.getString("outPutFileName");
            String strOutPutFileType = jsonInput.getString("outPutFileType");

            String strFilePathName = strPath + strOutPutFileName + "." + strOutPutFileType;
            File file = new File(strFilePathName);
            if(file.exists()){
                try {
                    byte[] b = Files.readAllBytes(Paths.get(strFilePathName));
                    // 文件转换为字节后，转换后的文件即可删除（pdf/ofd没用了）。
                    file.delete();
                    return Base64.getEncoder().encodeToString(b);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

}
