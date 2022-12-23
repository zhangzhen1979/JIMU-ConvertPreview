package com.thinkdifferent.convertpreview.entity;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.entity.mark.FirstPageMark;
import com.thinkdifferent.convertpreview.entity.mark.PngMark;
import com.thinkdifferent.convertpreview.entity.mark.TextMark;
import com.thinkdifferent.convertpreview.entity.writeback.WriteBack;
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
            entity.setFirstPageMark(FirstPageMark.get((Map<String,String>)parameters.get("firstPageMark")));
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
            outFileEncryptorEntity.setUserName(joOutEncry.optString("username"));
            outFileEncryptorEntity.setUserPassWord(joOutEncry.optString("userPassword"));
            outFileEncryptorEntity.setOwnerPassword(joOutEncry.optString("ownerPassword"));
            outFileEncryptorEntity.setCopy(joOutEncry.optBoolean("copy", false));
            outFileEncryptorEntity.setModify(joOutEncry.optBoolean("modigy", false));
            outFileEncryptorEntity.setPrint(joOutEncry.optBoolean("print", false));
            entity.setOutFileEncryptorEntity(outFileEncryptorEntity);
        }


        // 缩略图设置
        entity.setThumbnail(Thumbnail.convert(parameters));
        // 文件回写方式（回写路径[path]/回写接口[api]/ftp回写[ftp]）
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


    public OutFileEncryptorEntity getOutFileEncryptorEntity() {
        return outFileEncryptorEntity;
    }

    public void setOutFileEncryptorEntity(OutFileEncryptorEntity outFileEncryptorEntity) {
        this.outFileEncryptorEntity = outFileEncryptorEntity;
    }

    public boolean isPageNum() {
        return pageNum;
    }

    public void setPageNum(boolean pageNum) {
        this.pageNum = pageNum;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public Map<String, String> getInputHeaders() {
        return inputHeaders;
    }

    public void setInputHeaders(Map<String, String> inputHeaders) {
        this.inputHeaders = inputHeaders;
    }

    public String getOutPutFileName() {
        return outPutFileName;
    }

    public void setOutPutFileName(String outPutFileName) {
        this.outPutFileName = outPutFileName;
    }

    public String getOutPutFileType() {
        return outPutFileType;
    }

    public void setOutPutFileType(String outPutFileType) {
        this.outPutFileType = outPutFileType;
    }

    public WriteBackType getWriteBackType() {
        return writeBackType;
    }

    public void setWriteBackType(WriteBackType writeBackType) {
        this.writeBackType = writeBackType;
    }

    public WriteBack getWriteBack() {
        return writeBack;
    }

    public void setWriteBack(WriteBack writeBack) {
        this.writeBack = writeBack;
    }

    public List<Context> getContexts() {
        return contexts;
    }

    public void setContexts(List<Context> contexts) {
        this.contexts = contexts;
    }

    public String getCallBackURL() {
        return callBackURL;
    }

    public void setCallBackURL(String callBackURL) {
        this.callBackURL = callBackURL;
    }

    public Map<String, String> getCallBackHeaders() {
        return callBackHeaders;
    }

    public void setCallBackHeaders(Map<String, String> callBackHeaders) {
        this.callBackHeaders = callBackHeaders;
    }

    public FirstPageMark getFirstPageMark() {
        return firstPageMark;
    }

    public void setFirstPageMark(FirstPageMark firstPageMark) {
        this.firstPageMark = firstPageMark;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Input[] getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(Input[] inputFiles) {
        this.inputFiles = inputFiles;
    }

    public PngMark getPngMark() {
        return pngMark;
    }

    public void setPngMark(PngMark pngMark) {
        this.pngMark = pngMark;
    }

    public TextMark getTextMark() {
        return textMark;
    }

    public void setTextMark(TextMark textMark) {
        this.textMark = textMark;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
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
