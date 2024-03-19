package com.thinkdifferent.convertpreview.entity;

import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineBase;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.entity.mark.*;
import com.thinkdifferent.convertpreview.entity.writeback.WriteBack;
import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class ConvertDocEntity {
    /**
     * 输入类型（path/url）
     */
    private InputType inputType;
    /**
     * 输入的文件（数组）
     */
    private List<Input> inputFiles;
    /**
     * 输入的文件是否作为PDF/OFD的“附件”
     */
    private boolean inputAsAttach;
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
     * 输出文件是否只读（图片型版式文件）
     */
    private boolean outPutReadOnly;
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
    private PageNum pageNum;
    /**
     * 是否添加【版权声明】（PDF、OFD有效）
     */
    private CopyRight copyRight;
    /**
     * 首页水印（归档章）
     */
    private FirstPageMark firstPageMark;
    /**
     * 二维码/条码
     */
    private BarCode barCode;
    /**
     * 封面
     */
    private CoverPage coverPage;
    /**
     * 输入的附件（数组）
     */
    private List<Input> attachments;


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
     * 回写：zip参数
     */
    private ZipParam zipParam;
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
     * 输出文件页码范围
     */
    private String pageLimits;

    /**
     * 元数据JSON
     */
    private MetaData metaData;



    /**
     * 输入对象转换为标准对象
     *
     * @param parameters 输入对象
     * @return ce
     */
    public static ConvertDocEntity of(final Map<String, Object> parameters) throws InstantiationException, IllegalAccessException {
        log.info("接收到的数据：{}", parameters);
        ConvertDocEntity entity = new ConvertDocEntity();
        // 输入类型（path/url/ftp）
        entity.setInputType(InputType.valueOf(String.valueOf(parameters.get("inputType")).toUpperCase()));

        // 输入文件（"D:/temp/001.tif" http://... ftp://root:12345@0.0.0.1/a/a.txt）
        List<Input> listInputs = new ArrayList<>();
        if(parameters.get("inputFile") != null){

            String strInputFileName = null;
            if(parameters.containsKey("inputFileName") &&
                    parameters.get("inputFileName") != null){
                strInputFileName = String.valueOf(parameters.get("inputFileName"));
            }

            String strInputFileType = null;
            if(parameters.containsKey("inputFileType") &&
                    parameters.get("inputFileType") != null){
                strInputFileType = String.valueOf(parameters.get("inputFileType"));
            }

            if("url".equalsIgnoreCase(entity.getInputType().name())){
                if(StringUtils.isEmpty(strInputFileType)){
                    log.info("Error: InputType=[URL], The [inputFileType] can not be NULL");
                    return null;
                }
            }

            listInputs.add(
                    entity.getInputType().of(
                            String.valueOf(parameters.get("inputFile")),
                            strInputFileName,
                            strInputFileType,
                            false,
                            false
                    )
            );

        }else if(parameters.get("inputFiles") != null){
            // 输入多个pdf文件。后续进行合并
            JSONArray jaInputPdfs = JSONArray.fromObject(parameters.get("inputFiles"));
            for(int i=0;i<jaInputPdfs.size();i++){
                String strInputFileName = null;
                if(jaInputPdfs.getJSONObject(i).containsKey("inputFileName") &&
                        jaInputPdfs.getJSONObject(i).get("inputFileName") != null){
                    strInputFileName = jaInputPdfs.getJSONObject(i).getString("inputFileName");
                }

                String strInputFileType = null;
                if(jaInputPdfs.getJSONObject(i).containsKey("inputFileType") &&
                        jaInputPdfs.getJSONObject(i).get("inputFileType") != null){
                    strInputFileType = jaInputPdfs.getJSONObject(i).getString("inputFileType");
                }

                boolean duplexPrint = false;
                if(jaInputPdfs.getJSONObject(i).containsKey("duplexPrint") &&
                        jaInputPdfs.getJSONObject(i).get("duplexPrint") != null){
                    duplexPrint = jaInputPdfs.getJSONObject(i).getBoolean("duplexPrint");
                }
                boolean blankPageHaveNum = false;
                if(jaInputPdfs.getJSONObject(i).containsKey("blankPageHaveNum") &&
                        jaInputPdfs.getJSONObject(i).get("blankPageHaveNum") != null){
                    blankPageHaveNum = jaInputPdfs.getJSONObject(i).getBoolean("blankPageHaveNum");
                }

                listInputs.add(
                        entity.getInputType().of(
                                jaInputPdfs.getJSONObject(i).getString("inputFile"),
                                strInputFileName,
                                strInputFileType,
                                duplexPrint,
                                blankPageHaveNum
                        )
                );
            }
        }
        entity.setInputFiles(listInputs);


        // 输入文件是否作为附件加入到PDF/OFD中
        if(parameters.containsKey("inputAsAttach")){
            entity.setInputAsAttach(MapUtil.getBool(parameters, "inputAsAttach", false));
        }

        // 附件文件列表
        if(parameters.containsKey("attachments")){
            JSONArray jaAttchements = JSONArray.fromObject(parameters.get("attachments"));
            List<Input> listAttachs = new ArrayList<>();
            for(int i=0;i<jaAttchements.size();i++){

                String strInputFileName = null;
                if(jaAttchements.getJSONObject(i).containsKey("inputFileName") &&
                        jaAttchements.getJSONObject(i).get("inputFileName") != null){
                    strInputFileName = jaAttchements.getJSONObject(i).getString("inputFileName");
                }

                String strInputFileType = null;
                if(jaAttchements.getJSONObject(i).containsKey("inputFileType") &&
                        jaAttchements.getJSONObject(i).get("inputFileType") != null){
                    strInputFileType = jaAttchements.getJSONObject(i).getString("inputFileType");
                }

                listAttachs.add(
                        entity.getInputType().of(
                                jaAttchements.getJSONObject(i).getString("inputFile"),
                                strInputFileName,
                                strInputFileType,
                                false,
                                false
                        )
                );
            }
            entity.setAttachments(listAttachs);
        }

        // 输出文件的页码范围
        if(parameters.containsKey("pageLimits")){
            entity.setPageLimits(String.valueOf(parameters.get("pageLimits")));
        }

        // 是否添加页码。先取配置文件中的默认值。
        PageNum pageNum = new PageNum();
        pageNum.setEnable(ConvertDocConfigEngineBase.autoAddPageNumEnabled);
        pageNum.setType(ConvertDocConfigEngineBase.autoAddPageNumType);
        pageNum.setFontSize(ConvertDocConfigEngineBase.autoAddPageNumFontSize);
        pageNum.setFontColor(ConvertDocConfigEngineBase.autoAddPageNumFontColor);
        pageNum.setLocate(ConvertDocConfigEngineBase.autoAddPageNumLocate);
        pageNum.setMargins(ConvertDocConfigEngineBase.autoAddPageNumMargins);
        pageNum.setSwapPosition(ConvertDocConfigEngineBase.autoAddPageNumSwapPosition);
        pageNum.setStartNum(1);
        pageNum.setDigits(ConvertDocConfigEngineBase.autoAddPageNumDigits);

        // 是否添加【版权声明】。先取配置文件中的默认值。
        CopyRight copyRight = new CopyRight();
        copyRight.setEnable(ConvertDocConfigEngineBase.autoAddCopyRightEnabled);
        copyRight.setType(ConvertDocConfigEngineBase.autoAddCopyRightType);
        copyRight.setText(ConvertDocConfigEngineBase.autoAddCopyRightText);
        copyRight.setFontSize(ConvertDocConfigEngineBase.autoAddCopyRightFontSize);
        copyRight.setFontColor(ConvertDocConfigEngineBase.autoAddCopyRightFontColor);
        copyRight.setLocate(ConvertDocConfigEngineBase.autoAddCopyRightLocate);
        copyRight.setMargins(ConvertDocConfigEngineBase.autoAddCopyRightMargins);
        copyRight.setSwapPosition(ConvertDocConfigEngineBase.autoAddCopyRightSwapPosition);
        copyRight.setStartNum(1);

        String strWaterMarkParam = null;
        Map<String,Object> mapWaterMark = null;
        if (parameters.containsKey("waterMark")){
            strWaterMarkParam = parameters.get("waterMark").toString();
            if(!"".equalsIgnoreCase(strWaterMarkParam)){
                mapWaterMark = (Map<String,Object>)parameters.get("waterMark");
            }
        }

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

            if(mapWaterMark.containsKey("pageNum")){
                // 如果没开启页码相关设置，则走传入参数的设置
                Map<String, Object> mapPageNum = (Map<String, Object>)mapWaterMark.get("pageNum");
                pageNum.setEnable(MapUtil.getBool(mapPageNum, "enable", false));
                pageNum.setType("all");
                pageNum.setFontSize(MapUtil.getFloat(mapPageNum, "fontSize", 15f));
                pageNum.setFontColor(MapUtil.getStr(mapPageNum, "fontColor", "black"));
                pageNum.setLocate(MapUtil.getStr(mapPageNum, "locate", "TR"));
                pageNum.setMargins(MapUtil.getInt(mapPageNum, "margins", 30));
                pageNum.setSwapPosition(MapUtil.getBool(mapPageNum, "swapPosition", false));
                pageNum.setStartNum(MapUtil.getInt(mapPageNum, "startNum", 1));
                pageNum.setDigits(MapUtil.getInt(mapPageNum, "digits", 0));
            }

            if(mapWaterMark.containsKey("copyRight")){
                // 如果没开启【版权声明】相关设置，则走传入参数的设置
                Map<String, Object> mapPageNum = (Map<String, Object>)mapWaterMark.get("copyRight");
                copyRight.setEnable(MapUtil.getBool(mapPageNum, "enable", false));
                copyRight.setType("all");
                copyRight.setText(MapUtil.getStr(mapPageNum, "text", "CopyRight"));
                copyRight.setFontSize(MapUtil.getFloat(mapPageNum, "fontSize", 15f));
                copyRight.setFontColor(MapUtil.getStr(mapPageNum, "fontColor", "black"));
                copyRight.setLocate(MapUtil.getStr(mapPageNum, "locate", "TR"));
                copyRight.setMargins(MapUtil.getInt(mapPageNum, "margins", 30));
                copyRight.setSwapPosition(MapUtil.getBool(mapPageNum, "swapPosition", false));
                copyRight.setStartNum(MapUtil.getInt(mapPageNum, "startNum", 1));
            }

        }
        entity.setPageNum(pageNum);
        entity.setCopyRight(copyRight);


        // 首页水印（归档章）
        if(parameters.get("firstPageMark") != null){
            entity.setFirstPageMark(FirstPageMark.get((Map<String,Object>)parameters.get("firstPageMark")));
        }

        // 二维码/条码
        if(parameters.get("barcode") != null){
            entity.setBarCode(BarCode.get((Map<String,Object>)parameters.get("barcode")));
        }

        // 封面
        if(parameters.get("cover") != null){
            entity.setCoverPage(CoverPage.get((Map<String,Object>)parameters.get("cover")));
        }

        // 转换出来的文件名（不包含扩展名）（"001-online"）
        entity.setOutPutFileName(MapUtil.getStr(parameters, "outPutFileName"));
        // 文件输出格式， 默认 jpg
        entity.setOutPutFileType(MapUtil.getStr(parameters, "outPutFileType", "jpg"));
        // 是否生成只读的PDF或OFD
        if(parameters.get("outPutReadOnly") != null){
            entity.setOutPutReadOnly(MapUtil.getBool(parameters, "outPutReadOnly", false));
        }


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
            outFileEncryptorEntity.setCopy(joOutEncry.optBoolean("copy", true));
            // 是否可编辑。PDF、OFD均有效。
            outFileEncryptorEntity.setModify(joOutEncry.optBoolean("modify", true));
            // 是否可打印。PDF、OFD均有效。
            outFileEncryptorEntity.setPrint(joOutEncry.optBoolean("print", true));
            // 是否允许导出
            outFileEncryptorEntity.setExport(joOutEncry.optBoolean("export", true));
            // 有效期
            outFileEncryptorEntity.setValidPeriodStart(joOutEncry.optString("validPeriodStart", ""));
            outFileEncryptorEntity.setValidPeriodEnd(joOutEncry.optString("validPeriodEnd", ""));

            // 是否可以添加或修改文本注释并填写交互式表单字段，如果canModify()返回true，则创建或修改交互式表单字段（包括签名字段）。
            outFileEncryptorEntity.setModifyAnnotations(joOutEncry.optBoolean("modifyAnnotations", true));

            // PDF专用设置项
            // 是否可以插入/删除/旋转页面
            outFileEncryptorEntity.setAssembleDocument(joOutEncry.optBoolean("assembleDocument", true));
            // 是否可以填写交互式表单字段（包括签名字段）
            outFileEncryptorEntity.setFillInForm(joOutEncry.optBoolean("fillInForm", true));
            // 是否可以降级格式打印文档
            outFileEncryptorEntity.setPrintDegraded(joOutEncry.optBoolean("printDegraded", true));

            // OFD专用设置项
            // 允许打印的份数（允许打印时有效）
            outFileEncryptorEntity.setCopies(joOutEncry.optInt("copies", -1));
            // 是否允许添加签章
            outFileEncryptorEntity.setSignature(joOutEncry.optBoolean("signature", true));
            // 是否允许添加水印
            outFileEncryptorEntity.setWatermark(joOutEncry.optBoolean("watermark", true));

            entity.setOutFileEncryptorEntity(outFileEncryptorEntity);
        }


        // 缩略图设置
        entity.setThumbnail(Thumbnail.convert(parameters));
        // 文件回写方式（回写路径[path]/服务端路径[local]/回写接口[api]/ftp回写[ftp]）
        if (StringUtils.isNotBlank(MapUtil.getStr(parameters, "writeBackType"))){
            entity.setWriteBackType(WriteBackType.valueOf(MapUtil.getStr(parameters, "writeBackType").toUpperCase()));
        }else{
            entity.setWriteBackType(WriteBackType.valueOf("PATH"));
        }
        // 回写信息配置
        entity.setWriteBack(entity.getWriteBackType().convert(parameters));
        // 回写-zip参数配置
        if(parameters.containsKey("writeBack")){
            JSONObject joWriteBack = JSONObject.fromObject(parameters.get("writeBack"));
            if(joWriteBack.containsKey("zip")){
                JSONObject joZip = joWriteBack.getJSONObject("zip");
                entity.setZipParam(ZipParam.of(joZip));
            }
        }
        // 双层PDF文本内容
        entity.setContexts(Context.ofList(parameters.get("context")));
        // 回调
        entity.setCallBackURL(MapUtil.getStr(parameters, "callBackURL"));
        if (parameters.get("callBackHeaders") != null) {
            entity.setCallBackHeaders((Map<String, String>) parameters.get("callBackHeaders"));
        }


        // 元数据JSON
        if(parameters.get("metaData") != null){
            entity.setMetaData(MetaData.get((Map<String, Object>)parameters.get("metaData")));
        }


        log.info("转换后对象:{}", entity);
        return entity;
    }

}
