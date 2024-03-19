package com.thinkdifferent.convertpreview.engine.impl.onlyOffice;

import lombok.extern.log4j.Log4j2;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;

import java.util.Map;


/**
 * @BelongsProject: onlyoffice-demo
 * @BelongsPackage: com.onlyoffice.onlyoffice.tools
 * @Author: TongHui
 * @CreateTime: 2023-07-30 18:39
 * @Description: TODO
 * @Version: 1.0
 */
@Log4j2
public class JWTUtil {

    public static String createToken(Map<String, Object> map, String secret) {
        try {
            Signer signer = HMACSigner.newSHA256Signer(secret);
            JWT jwt = new JWT();
            for (String key : map.keySet()) {
                jwt.addClaim(key, map.get(key));
            }
            return JWT.getEncoder().encode(jwt, signer);
        } catch (Exception | Error e) {
            log.error(e);
            return "";
        }
    }
}
