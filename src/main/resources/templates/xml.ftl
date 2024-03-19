<!DOCTYPE html>
<html lang="zh-cn">
<head>
    <meta charset="utf-8"/>
    <title>XML文件预览</title>
    <#include "*/commonHeader.ftl">

</head>
<body>

<pre id="myPre"></pre>
<script type="text/javascript">
    var xmlUrl = '${xmlUrl}';

    var xhr = new XMLHttpRequest();
    xhr.open('GET', xmlUrl, true);
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            context = xhr.responseText;
            document.write("<xmp>" + context + "</xmp>")
        }
        return 1;
    };
    xhr.send(null);

    window.onload = function () {
        /*初始化水印*/
        initWaterMark();
    };
</script>

</body>
</html>
