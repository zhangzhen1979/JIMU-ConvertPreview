<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, user-scalable=yes, initial-scale=1.0">
    <title>预览</title>
    <#include "commonHeader.ftl">
</head>

<body>
<#--<#if pdfUrl?contains("http://") || pdfUrl?contains("https://")>-->
<#--    <#assign finalUrl="${pdfUrl}">-->
<#--<#else>-->
<#--    <#assign finalUrl="${baseUrl}${pdfUrl}">-->
<#--</#if>-->
<iframe src="" width="100%" frameborder="0"></iframe>

<#-- excel 不可切换 -->
<#if "false" == blnExcel && "true" == blnChangeType >
    <img src="/images/jpg.svg" width="63" height="63"
         style="position: fixed; cursor: pointer; top: 40%; right: 48px; z-index: 999;" alt="使用图片预览" title="使用图片预览"
         onclick="goForImage()"/>
</#if>
</body>

<#--<div id="tempUrl">${tempUrl}</div>-->
<#--<div id="downloadUrl">${downloadUrl}</div>-->
<script type="text/javascript">

    var htmlUrl = '${htmlUrl}';
    console.log('html内容：', htmlUrl)
    if (htmlUrl) {
        document.getElementsByTagName('iframe')[0].src = htmlUrl;
    } else {
        var baseUrl = '${baseUrl}'.endsWith('/') ? '${baseUrl}' : '${baseUrl}' + '/';
        var url = baseUrl + 'api/download?urlPath=' + encodeURIComponent('${pdfUrl}');
        document.getElementsByTagName('iframe')[0].src = "${baseUrl}/pdfjs/web/viewer.html?file=" + encodeURIComponent(url)
            + "&disablepresentationmode=${pdfPresentationModeDisable}&disableopenfile=${pdfOpenFileDisable}"
            + "&disableprint=${pdfPrintDisable}&disabledownload=${pdfDownloadDisable}&disablebookmark=${pdfBookmarkDisable}&keyword=${keyword}";
    }
    document.getElementsByTagName('iframe')[0].height = document.documentElement.clientHeight - 10;
    /**
     * 页面变化调整高度
     */
    window.onresize = function () {
        var fm = document.getElementsByTagName("iframe")[0];
        fm.height = window.document.documentElement.clientHeight - 10;
    }

    function goForImage() {
        var url = window.location.href
        if (url.indexOf("outType=pdf") !== -1) {
            url = url.replace("outType=pdf", "outType=officePicture");
        } else {
            url = url + "&outType=officePicture";
        }
        window.location.href = url;
    }

    /*初始化水印*/
    window.onload = function () {
        initWaterMark();
    }
    document.body.parentNode.style.overflowX = "hidden"
    document.body.parentNode.style.overflowY = "hidden"
</script>
</html>
