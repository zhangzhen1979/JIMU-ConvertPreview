package com.thinkdifferent.convertpreview.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class FontUtil {

    public static String getSystemFontPathFile(String strFileName){

        if(StringUtils.isEmpty(strFileName)){
            strFileName = "simsun.ttf";
        }else{
            strFileName = SystemUtil.beautifulFilePath(strFileName);
            if(strFileName.contains("/")){
                return strFileName;
            }
        }

        String strFontPathFile;

        if (cn.hutool.system.SystemUtil.getOsInfo().isWindows()) {
            // 读取Windows字体文件夹
            strFontPathFile = SystemUtil.beautifulPath(System.getenv("WINDIR")) + "Fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }

            // 读取User文件夹下的字体文件夹
            strFontPathFile = SystemUtil.beautifulPath(System.getenv("USERPROFILE")) +
                    "AppData/Local/Microsoft/Windows/Fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }

        } else if (cn.hutool.system.SystemUtil.getOsInfo().isLinux()) {
            strFontPathFile = "/usr/share/fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }
        } else if (cn.hutool.system.SystemUtil.getOsInfo().isMacOsX()) {
            strFontPathFile = "/Library/Fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }

            strFontPathFile = "/System/Library/Fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }

            strFontPathFile =  SystemUtil.beautifulPath(System.getProperty("user.home")) + "/Library/Fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }
        } else if (cn.hutool.system.SystemUtil.getOsInfo().isHpUx()) {
            strFontPathFile = SystemUtil.beautifulPath(System.getProperty("user.home")) + "/.local/share/fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }

            strFontPathFile = SystemUtil.beautifulPath(System.getProperty("user.home")) + "/.fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }

            strFontPathFile = "/usr/share/fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }

            strFontPathFile = "/usr/local/share/fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }
        } else {
            strFontPathFile = "/usr/share/fonts/" + strFileName;
            if(isExist(strFontPathFile)){
                return strFontPathFile;
            }
        }

        return strFontPathFile;
    }

    private static boolean isExist(String strFontPathFile){
        File fileFont = new File(strFontPathFile);
        if(fileFont.exists() && fileFont.length() > 0){
            return true;
        }
        return false;
    }

}
