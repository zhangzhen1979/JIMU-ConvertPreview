package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * ecology 相关工具类
 */
@Log4j2
public class EcologyUploadUtil {

    /**
     * ecology系统发放的授权许可证(appid)
     */
    @Value("${convert.ecology.appid}")
    private static String APPID;

    /**
     * 模拟缓存服务
     */
    private static final Map<String, String> SYSTEM_CACHE = new HashMap<String, String>();

    /**
     * TOKEN 缓存
     * 5min 过期
     */
    private static final Cache<String, String> TOKEN_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .maximumSize(5)
            .build();


    /**
     * @param address           服务IP地址         http://10.115.92.26/
     * @param api               文件上传地址    /api/doc/upload/uploadFile2Doc
     * @param localFilePath     本地文件路径
     * @param ecologyFilePathId ecology文件路径 ID
     * @return
     */
    public static JSONObject uploadFile2Ecology(String address, String api, String localFilePath, String ecologyFilePathId, String fileName) {
        if (StringUtils.isBlank(fileName)){
            fileName = localFilePath.substring(localFilePath.lastIndexOf(File.separator));
        }
        //ECOLOGY返回的token
        String token = SYSTEM_CACHE.get("SERVER_TOKEN");
        log.info("+++token1++++" + token);
        if ("".equals(token) || null == token) {
            token = (String) getToken(address).get("token");
        }
        log.info("+++token2++++" + token);
        String spk = SYSTEM_CACHE.get("SERVER_PUBLIC_KEY");
        //封装请求头参数
        RSA rsa = new RSA(null, spk);
        //对用户信息进行加密传输,暂仅支持传输OA用户ID
        String encryptUserid = rsa.encryptBase64("1", CharsetUtil.CHARSET_UTF_8, KeyType.PublicKey);

        log.info("+++encryptUserid++++" + encryptUserid);

        //调用ECOLOGY系统接口
        String data = HttpRequest.post(address + api)
                .header("appid", APPID)
                .header("token", token)
                .header("userid", encryptUserid)
                .header("charset", "utf-8")
                .form("file", new File(localFilePath))
                .form("category", ecologyFilePathId) // 文档目录id（后台配置后，为相对固定值）
                .form("filename", fileName)
                .execute().body();

        log.info("+++++文件上传返回数据++++++" + data);
        return JSONObject.fromObject(data);
    }

    /**
     * 第一步：
     * <p>
     * 调用ecology注册接口,根据appid进行注册,将返回服务端公钥和Secret信息
     */
    public static Map<String, Object> testRegist(String address) {

        //获取当前系统RSA加密的公钥
        RSA rsa = new RSA();
        String publicKey = rsa.getPublicKeyBase64();
        String privateKey = rsa.getPrivateKeyBase64();

        // 客户端RSA私钥
        SYSTEM_CACHE.put("LOCAL_PRIVATE_KEY", privateKey);
        // 客户端RSA公钥
        SYSTEM_CACHE.put("LOCAL_PUBLIC_KEY", publicKey);

        //调用ECOLOGY系统接口进行注册

        HttpResponse execute = HttpRequest.post(address + "api/ec/dev/auth/regist")
                .header("appid", APPID)
                .header("cpk", publicKey)
                .timeout(2000)
                .execute();

        String data = execute.body();

        // 打印ECOLOGY响应信息
        log.info("testRegist()：" + data);
        Map<String, Object> datas = JSONUtil.parseObj(data);

        //ECOLOGY返回的系统公钥
        SYSTEM_CACHE.put("SERVER_PUBLIC_KEY", (String) datas.get("spk"));
        //ECOLOGY返回的系统密钥
        SYSTEM_CACHE.put("SERVER_SECRET", StringUtils.isBlank(datas.get("secrit").toString()) ? "" : ((String) datas.get("secrit")));
        return datas;
    }

    public static Map<String, Object> getToken(String address) {
        return getToken(address, 0);
    }
    /**
     * 第二步：
     * <p>
     * 通过第一步中注册系统返回信息进行获取token信息
     */
    public static Map<String, Object> getToken(String address, int retryNum) {
        if (retryNum > 3){
            log.error("获取ecology异常");
            return new HashMap<>();
        }
        // 从系统缓存或者数据库中获取ECOLOGY系统公钥和Secret信息
        String secret = SYSTEM_CACHE.get("SERVER_SECRET");
        String spk = SYSTEM_CACHE.get("SERVER_PUBLIC_KEY");

        // 如果为空,说明还未进行注册,调用注册接口进行注册认证与数据更新
        if (Objects.isNull(secret) || Objects.isNull(spk)) {
            testRegist(address);
            // 重新获取最新ECOLOGY系统公钥和Secret信息
            secret = SYSTEM_CACHE.get("SERVER_SECRET");
            spk = SYSTEM_CACHE.get("SERVER_PUBLIC_KEY");
        }

        // 公钥加密,所以RSA对象私钥为null
        RSA rsa = new RSA(null, spk);
        //对秘钥进行加密传输，防止篡改数据
        String encryptSecret = rsa.encryptBase64(secret, CharsetUtil.CHARSET_UTF_8, KeyType.PublicKey);

        //调用ECOLOGY系统接口进行注册
        String data = HttpRequest.post(address + "api/ec/dev/auth/applytoken")
                .header("appid", APPID)
                .header("secret", encryptSecret)
                .header("time", "3600")
                .execute()
                .body();

        log.info("testGetoken()：" + data);
        Map<String, Object> datas = JSONUtil.parseObj(data);
        if (StringUtils.equals("false", datas.get("status").toString())){
            // false 时，重新刷新
            SYSTEM_CACHE.remove("SERVER_SECRET");
            SYSTEM_CACHE.remove("SERVER_PUBLIC_KEY");
            return getToken(address, ++retryNum);
        }

        //ECOLOGY返回的token
        // 为Token缓存设置过期时间
        // SYSTEM_CACHE.put("SERVER_TOKEN", StringUtils.isBlank(datas.get("token").toString()) ? "" : ((String) datas.get("token")));

        return datas;
    }


    /**
     * @param address 服务IP地址         http://10.115.92.26/
     * @return
     */
    public static String resetEcologyCache(String address) throws ExecutionException, InterruptedException {
        //ECOLOGY返回的token
        String token = TOKEN_CACHE.get("SERVER_TOKEN", () -> (String) getToken(address).get("token"));
        log.info("+++token1++++" + token);
        String spk = SYSTEM_CACHE.get("SERVER_PUBLIC_KEY");
        //封装请求头参数
        RSA rsa = new RSA(null, spk);
        //对用户信息进行加密传输,暂仅支持传输OA用户ID
        String encryptUserid = rsa.encryptBase64("1", CharsetUtil.CHARSET_UTF_8, KeyType.PublicKey);

        log.info("+++encryptUserid++++" + encryptUserid);

        // 停用缓存
        String data = HttpRequest.get(address + "gome/gomeClearcache.jsp")
                .header("appid", APPID)
                .header("token", token)
                .header("userid", encryptUserid)
                .header("charset", "utf-8")
                .header("skipsession", "0")
                .execute().body();

        log.info("++++++停用缓存+++++data++++++" + data);

        return data;
    }
}
