package com.thinkdifferent.convertpreview.dao;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author ltian
 * @version 1.0
 * @date 2023/10/27 18:00
 */
public interface ConvertDao {
    <T extends ConvertBase> OutputStream convert(InputStream inputStream, T convertEntity);
}


