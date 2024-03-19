<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, user-scalable=yes, initial-scale=1.0">
    <title>PDF预览文档</title>
    <#include "commonHeader.ftl">
</head>

<body>
<iframe src="" width="100%" frameborder="0"></iframe>
<div class="pdf-loading">
    <img src="/images/loading.gif"/>
</div>

</body>
<style>
    .pdf-loading {
        z-index: 1;
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
    }
</style>
<#--<div id="tempUrl">${tempUrl}</div>-->
<#--<div id="downloadUrl">${downloadUrl}</div>-->
<script type="text/javascript">

    var htmlUrl = '${htmlUrl}';
    var blnOfd = '${blnOfd}';
    console.log('html内容：', htmlUrl)
    if (htmlUrl) {
        if ("true" === blnOfd){
            if(IsPhone()){
                document.getElementsByTagName('iframe')[0].src = "/ofd/index.html?file=" + encodeURIComponent(htmlUrl)+"&scale=width";
            }else{
                document.getElementsByTagName('iframe')[0].src = "/ofd/index.html?file=" + encodeURIComponent(htmlUrl)
                    + "&disableprint=${pdfPrintDisable}&disabledownload=${pdfDownloadDisable}";
            }
        } else{
            document.getElementsByTagName('iframe')[0].src = htmlUrl;
        }
    } else {
        var url = '/api/download?urlPath=' + encodeURIComponent('${pdfUrl}');
        // pdf 遮盖
        var uid = '&uid=${uid}'
        var pdfCover = "";
        if (uid.length > 5) {
            pdfCover = "&pdfCover=true";
        }
        document.getElementsByTagName('iframe')[0].src = "/pdfjs/web/viewer.html?file=" + encodeURIComponent(url)
            + pdfCover + uid
            + "&disablepresentationmode=${pdfPresentationModeDisable}"
            + "&disableprint=${pdfPrintDisable}&disabledownload=${pdfDownloadDisable}&keyword=${keyword}";
    }
    document.getElementsByTagName('iframe')[0].height = document.documentElement.clientHeight - 10;

    var iframe = document.getElementsByTagName('iframe')[0]
    var target = document.querySelector('.pdf-loading')
    var parent = target.parentElement
    iframe.onload = () => {
        parent.removeChild(target)
    }
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

    function IsPhone() {
        var info = navigator.userAgent;
        //通过正则表达式的test方法判断是否包含“Mobile”字符串
        var isPhone = /mobile/i.test(info);
        //如果包含“Mobile”（是手机设备）则返回true
        return isPhone;
    }

    /*初始化水印*/
    window.onload = function () {
        initWaterMark();
    }
    document.body.parentNode.style.overflowX = "hidden"
    document.body.parentNode.style.overflowY = "hidden"
</script>
</html>
