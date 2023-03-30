package com.thinkdifferent.convertpreview.entity;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.entity.mark.BarCode;
import com.thinkdifferent.convertpreview.entity.mark.FirstPageMark;
import com.thinkdifferent.convertpreview.entity.mark.PngMark;
import com.thinkdifferent.convertpreview.entity.mark.TextMark;
import com.thinkdifferent.convertpreview.entity.writeback.WriteBack;
import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 转换对象
 *
 * @author ltian
 * @version 1.0
 * @date 2022/4/13 13:49
 */
@Log4j2
@ToString
@Data
public class ConvertEntity {
    /**
     * 输入类型（path/url）
     */
    private InputType inputType;
    /**
     * 输入的文件（数组）
     */
    private Input[] inputFiles;
    /**
     * url 请求头
     */
    private Map<String, String> inputHeaders;
    /**
     * 转换出来的文件名（不包含扩展名）
     */
    private String outPutFileName;
    /**
     * 文件输出格式, 默认jpg
     */
    private String outPutFileType;
    /**
     * 缩略图设置
     */
    private Thumbnail thumbnail;
    /**
     * 图片水印
     */
    private PngMark pngMark;
    /**
     * 文字水印
     */
    private TextMark textMark;
    /**
     * 是否添加页码（PDF、OFD有效）
     */
    private boolean pageNum;
    /**
     * 首页水印（归档章）
     */
    private FirstPageMark firstPageMark;
    /**
     * 二维码/条码
     */
    private BarCode barCode;
    /**
     * 水印透明度
     */
    private float alpha;
    /**
     * 文件回写方式（回写路径[path]/回写接口[api]/ftp回写[ftp]）
     */
    private WriteBackType writeBackType;
    /**
     * 回写接口或回写路径
     */
    private WriteBack writeBack;
    /**
     * 双层PDF文字内容
     */
    private List<Context> contexts;
    /**
     * 回调地址
     */
    private String callBackURL;
    /**
     * 回调请求头
     */
    private Map<String, String> callBackHeaders;
    /**
     * OFD加密设置
     */
    private OutFileEncryptorEntity outFileEncryptorEntity;

    /**
     * 输入对象转换为标准对象
     *
     * @param parameters 输入对象
     * @return ce
     */
    public static ConvertEntity of(final Map<String, Object> parameters) throws InstantiationException, IllegalAccessException {
        log.info("接收到的数据：{}", parameters);
        ConvertEntity entity = new ConvertEntity();
        // 输入类型（path/url/ftp）
        entity.setInputType(InputType.valueOf(String.valueOf(parameters.get("inputType")).toUpperCase()));

        // 输入文件（"D:/temp/001.tif" http://... ftp://root:12345@0.0.0.1/a/a.txt）
        if(parameters.get("inputFile") != null){
            Input[] inputs = new Input[1];

            String strInputFileType = String.valueOf(parameters.get("inputFileType"));

            if("url".equalsIgnoreCase(entity.getInputType().name())){
                if(StringUtils.isEmpty(strInputFileType)){
                    log.info("Error: InputType=[URL], The [inputFileType] can not be NULL");
                    return null;
                }
            }

            if(StringUtils.isEmpty(strInputFileType)){
                strInputFileType = String.valueOf(parameters.get("inputFile"));
                strInputFileType = strInputFileType.substring(strInputFileType.lastIndexOf("."));
            }

            inputs[0] = entity.getInputType().of(
                    String.valueOf(parameters.get("inputFile")),
                    strInputFileType
            );
            entity.setInputFiles(inputs);
        }else if(parameters.get("inputFiles") != null){
            // 输入多个pdf文件。后续进行合并
            JSONArray jaInputPdfs = JSONArray.fromObject(parameters.get("inputFiles"));
            Input[] inputs = new Input[jaInputPdfs.size()];
            for(int i=0;i<jaInputPdfs.size();i++){
                String strInputFileType = jaInputPdfs.getJSONObject(i).getString("inputFileType");
                if(StringUtils.isEmpty(strInputFileType)){
                    strInputFileType = jaInputPdfs.getJSONObject(i).getString("inputFile");
                    strInputFileType = strInputFileType.substring(strInputFileType.lastIndexOf("."));
                }

                inputs[i] = entity.getInputType().of(
                        jaInputPdfs.getJSONObject(i).getString("inputFile"),
                        strInputFileType
                );
            }
            entity.setInputFiles(inputs);
        }

        Map<String,Object> mapWaterMark = (Map<String,Object>)parameters.get("waterMark");
        if(mapWaterMark != null){
            entity.setAlpha(Float.valueOf(MapUtil.getStr(mapWaterMark, "alpha", "1f")));
            // 图片水印
            if(mapWaterMark.containsKey("pic")){
                PngMark pngMark = new PngMark();
                entity.setPngMark(pngMark.get((Map<String,String>)mapWaterMark.get("pic")));
            }
            // 文字水印
            if(mapWaterMark.containsKey("text")){
                TextMark textMark = new TextMark();
                entity.setTextMark(textMark.get((Map<String,String>)mapWaterMark.get("text")));
            }
            // 是否添加页码
            if(mapWaterMark.containsKey("pageNum")){
                entity.setPageNum((boolean)mapWaterMark.get("pageNum"));
            }
        }


        // 首页水印（归档章）
        if(parameters.get("firstPageMark") != null){
            entity.setFirstPageMark(FirstPageMark.get((Map<String,Object>)parameters.get("firstPageMark")));
        }

        // 二维码/条码
        if(parameters.get("barcode") != null){
            entity.setBarCode(BarCode.get((Map<String,Object>)parameters.get("barcode")));
        }


        // 转换出来的文件名（不包含扩展名）（"001-online"）
        entity.setOutPutFileName(MapUtil.getStr(parameters, "outPutFileName"));
        // 文件输出格式， 默认 jpg
        entity.setOutPutFileType(MapUtil.getStr(parameters, "outPutFileType", "jpg"));


        // 输出的PDF/OFD加密设置
        if(parameters.get("outEncry") != null){
            JSONObject joOutEncry = JSONObject.fromObject(parameters.get("outEncry"));
            OutFileEncryptorEntity outFileEncryptorEntity = new OutFileEncryptorEntity();

            outFileEncryptorEntity.setEncry(true);
            // 原有通用字段
            // 用户名。PDF、OFD共用。为兼容【超越版式办公套件】，OFD文件建议传入固定值admin
            outFileEncryptorEntity.setUserName(joOutEncry.optString("username", ""));
            // 用户密码。PDF、OFD共用
            outFileEncryptorEntity.setUserPassWord(joOutEncry.optString("userPassword", ""));
            // 文档所有者密码。PDF有效。
            outFileEncryptorEntity.setOwnerPassword(joOutEncry.optString("ownerPassword", ""));
            // 是否可以复制。PDF、OFD均有效。
            outFileEncryptorEntity.setCopy(joOutEncry.optBoolean("copy", false));
            // 是否可编辑。PDF、OFD均有效。
            outFileEncryptorEntity.setModify(joOutEncry.optBoolean("modify", false));
            // 是否可打印。PDF、OFD均有效。
            outFileEncryptorEntity.setPrint(joOutEncry.optBoolean("print", false));

            // 是否可以添加或修改文本注释并填写交互式表单字段，如果canModify()返回true，则创建或修改交互式表单字段（包括签名字段）。
            outFileEncryptorEntity.setModifyAnnotations(joOutEncry.optBoolean("modifyAnnotations", false));

            // PDF专用设置项
            // 是否可以插入/删除/旋转页面
            outFileEncryptorEntity.setAssembleDocument(joOutEncry.optBoolean("assembleDocument", false));
            // 是否可以填写交互式表单字段（包括签名字段）
            outFileEncryptorEntity.setFillInForm(joOutEncry.optBoolean("fillInForm", false));
            // 是否可以降级格式打印文档
            outFileEncryptorEntity.setPrintDegraded(joOutEncry.optBoolean("printDegraded", false));

            // OFD专用设置项
            // 允许打印的份数（允许打印时有效）
            outFileEncryptorEntity.setCopies(joOutEncry.optInt("copies", -1));
            // 是否允许添加签章
            outFileEncryptorEntity.setSignature(joOutEncry.optBoolean("signature", false));
            // 是否允许添加水印
            outFileEncryptorEntity.setWatermark(joOutEncry.optBoolean("watermark", false));
            // 是否允许导出
            outFileEncryptorEntity.setExport(joOutEncry.optBoolean("export", false));
            // 有效期
            outFileEncryptorEntity.setValidPeriodStart(joOutEncry.optString("validPeriodStart", ""));
            outFileEncryptorEntity.setValidPeriodEnd(joOutEncry.optString("validPeriodEnd", ""));

            entity.setOutFileEncryptorEntity(outFileEncryptorEntity);
        }


        // 缩略图设置
        entity.setThumbnail(Thumbnail.convert(parameters));
        // 文件回写方式（回写路径[path]/服务端路径[local]/回写接口[api]/ftp回写[ftp]）
        entity.setWriteBackType(WriteBackType.valueOf(MapUtil.getStr(parameters, "writeBackType", "path").toUpperCase()));
        // 回写信息配置
        entity.setWriteBack(entity.getWriteBackType().convert(parameters));
        // 双层PDF文本内容
        entity.setContexts(Context.ofList(parameters.get("context")));
        // 回调
        entity.setCallBackURL(MapUtil.getStr(parameters, "callBackURL"));
        if (parameters.get("callBackHeaders") != null) {
            entity.setCallBackHeaders((Map<String, String>) parameters.get("callBackHeaders"));
        }
        log.info("转换后对象:{}", entity);
        return entity;
    }

    /**
     * @return 输入文件名
     */
    public String getInputFileName(){
        return  Arrays.stream(inputFiles)
                .map(input-> {
                    File inputFile = input.getInputFile();
                    return FileUtil.exist(inputFile) ? inputFile.getName() : "" ;
                }).collect(Collectors.joining(","));
    }
}
