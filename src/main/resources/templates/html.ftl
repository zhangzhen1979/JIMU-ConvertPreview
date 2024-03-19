<!DOCTYPE html>
<html lang="zh-cn">
<head>
    <meta charset="utf-8"/>
    <title>HTML文件预览</title>
    <#include "*/commonHeader.ftl">
    <style>
        *::-webkit-scrollbar {
            display: none;
        }
    </style>
</head>
<body>
<iframe width="100%" height="100%" id="previewIframe"></iframe>
<script type="text/javascript">
    var htmlUrl = '${htmlUrl}';

    document.getElementById("previewIframe").src = htmlUrl;
    window.onload = function () {
        /*初始化水印*/
        initWaterMark();
    };
</script>
</body>
</html>
