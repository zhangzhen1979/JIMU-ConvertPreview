<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <title>预览</title>
    <script src="/js/lazyload.js"></script>
    <#include "commonHeader.ftl">
    <style>
        body {
            background-color: #404040;
        }

        .container {
            width: 100%;
            height: 100%;
        }

        .img-area {
            text-align: center;
            height: 100%;
        }

    </style>
</head>
<body>
<div class="container">
    <#list imgUrls as img>
        <div class="img-area">
            <img class="my-photo" alt="loading" data-src="${img}" src="/images/loading.gif">
        </div>
        <br/>
    </#list>
</div>
<#if "false" == blnExcel && "true" == blnChangeType >
<img src="/images/pdf.svg" width="63" height="63" style="position: fixed; cursor: pointer; top: 40%; right: 48px;
     z-index: 999;" alt="使用PDF预览" title="使用PDF预览" onclick="changePreviewType('pdf')"/>
</#if>
<script>
    document.body.parentNode.style.overflowX= "hidden"
    window.onload = function () {
        /*初始化水印*/
        initWaterMark();
        checkImgs();
        initWaterMark();
    };
    // window.onload = function (){
    //     initWaterMark();
    //
    // };
    window.onscroll = throttle(checkImgs);

    function changePreviewType(previewType) {
        var url = window.location.href;
        if (url.indexOf("outType=officePicture") !== -1) {
            url = url.replace("outType=officePicture", "outType=pdf");
        } else {
            url = url + "&outType=pdf";
        }
        if ('allImages' === previewType) {
            window.open(url)
        } else {
            window.location.href = url;
        }
    }
</script>
</body>
</html>