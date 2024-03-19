package com.thinkdifferent.convertpreview.utils;

/**
 * @author 张镇
 * @version 1.0
 * @date 2023-11-17
 */
public class DataUtil {

    /**
     * 按照输入的位数，自动补0
     * @param intInput   输入的整数值
     * @param intMaxLen  补0后的字符串位数。如果输入值为0，则不补0。
     * @return  补0后的字符串。如：003
     */
    public static String autoAddZero(int intInput, int intMaxLen) {
        String strValue = String.valueOf(intInput);
        if(intMaxLen >0){
            int intInputLen = strValue.length();

            if(intInputLen < intMaxLen){
                for(int i=0; i<(intMaxLen - intInputLen); i++){
                    strValue = "0" + strValue;
                }
            }
        }
        return strValue;
    }

}
