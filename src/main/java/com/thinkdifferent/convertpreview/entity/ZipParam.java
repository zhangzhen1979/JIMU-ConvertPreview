package com.thinkdifferent.convertpreview.entity;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
//@Accessors(chain = true)
@Log4j2
public class ZipParam {
    private boolean hasExt;
    private String password;
    private boolean randompwd;
    private int randompwdlen;

    /**
     * 输入对象转换
     *
     * @param jsonObject 输入对象
     * @return 标准entity
     */
    @SneakyThrows
    public static ZipParam of(JSONObject jsonObject) {
        log.debug("接收参数:{}", jsonObject);
        ZipParam zipParam = new ZipParam();
        if (!Objects.isNull(jsonObject)) {
            zipParam.setHasExt(jsonObject.optBoolean("hasExt", false));
            zipParam.setPassword(jsonObject.optString("password", null));
            zipParam.setRandompwd(jsonObject.optBoolean("randompwd", false));
            zipParam.setRandompwdlen(jsonObject.optInt("randompwdlen", 6));
        }

        return zipParam;
    }


    /**
     * 根据参数，判断是否zip压缩；是否压缩加密
     * @param zipParam          zip参数的JSON对象
     * @param strDest           目标文件路径和文件名
     * @param fileTemp          临时文件File对象
     * @param strFileNameInzip  zip包中的文件名
     * @return
     * @throws ZipException
     */
    public static Map<String,Object> checkToZip(
            ZipParam zipParam,
            String strDest,
            File fileTemp,
            String strFileNameInzip
    ) throws ZipException {

        String strPWD = zipParam.getPassword();
        boolean blnRandomPWD = zipParam.isRandompwd();
        int intRandomPWDLen = zipParam.getRandompwdlen();

        boolean blnPwd = false;
        if(!StringUtils.isEmpty(strPWD) || blnRandomPWD){
            blnPwd = true;
        }

        strPWD = zipDir(
                fileTemp.getAbsolutePath(),
                strDest,
                blnPwd,
                strPWD,
                blnRandomPWD,
                intRandomPWDLen,
                strFileNameInzip
        );

        Map<String,Object> mapReturn = new HashMap<>();
        mapReturn.put("fileDest", new File(strDest));
        mapReturn.put("pwd", strPWD);

        return mapReturn ;
    }


    /**
     * 将指定文件压缩为zip文件
     * @param strSourceFile    需要压缩的文件夹
     * @param strZipFile       生成的zip文件的路径和文件名（文件名可以不包含扩展名）
     * @param blnPwd           zip是否需要密码
     * @param strPassword      zip密码
     * @param blnRandomPwd     是否生成随机密码（如果设置了zip密码，则此项失效）
     * @param intRandomPwdLen  随机密码长度（需要开启【blnRandomPwd】。如果设置了zip密码，则此项失效）
     * @param strFileNameInZip zip包中的文件名
     * @return  返回动态生成的随机密码。【blnRandomPwd】为true时有效。
     * @throws ZipException
     */
    public static String zipDir(
            String strSourceFile,
            String strZipFile,
            boolean blnPwd,
            String strPassword,
            boolean blnRandomPwd,
            int intRandomPwdLen,
            String strFileNameInZip
    ) throws ZipException {

        ZipFile zip = new ZipFile(strZipFile);
        File file = zip.getFile();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (file.exists()) {
            file.delete();
        }

        ZipParameters para = new ZipParameters();
        para.setCompressionMethod(CompressionMethod.DEFLATE);
        para.setCompressionLevel(CompressionLevel.NORMAL);
        if(StringUtils.isNotEmpty(strFileNameInZip)){
            para.setFileNameInZip(strFileNameInZip);
        }

        if(blnPwd){
            para.setEncryptFiles(true);
            para.setEncryptionMethod(EncryptionMethod.AES);
            para.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

            if(StringUtils.isBlank(strPassword) && blnRandomPwd){
                if(intRandomPwdLen == 0){
                    intRandomPwdLen = 6;
                }
                strPassword = RandomStringUtils.randomAlphanumeric(intRandomPwdLen);
            }
            zip.setPassword(strPassword.toCharArray());
        }

        zip.addFile(new File(strSourceFile), para);

        return strPassword;
    }

}