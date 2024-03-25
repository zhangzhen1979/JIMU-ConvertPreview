<!DOCTYPE html>
<html lang="zh-cn">
<head>
    <meta charset="utf-8"/>
    <title>文件预览</title>
    <#include "*/commonHeader.ftl">

</head>
<body>

<script type="text/javascript">
    var xmlUrl = '${xmlUrl}';

    var xhr = new XMLHttpRequest();
    xhr.open('GET', xmlUrl, true);
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            context = xhr.responseText;
            var xmp = document.createElement("xmp");
            xmp.textContent = context;
            document.body.append(xmp)
            // document.write("<xmp>" + context + "</xmp>")
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
