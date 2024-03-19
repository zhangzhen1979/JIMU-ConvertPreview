<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <title>图片预览</title>
    <link rel="stylesheet" href="/css/viewer.min.css">
    <script src="/js/viewer.min.js"></script>
    <#include "commonHeader.ftl">
    <style>
        body {
            background-color: #404040;
        }

        #image {
            height: 100%;
            margin: 0 auto;
            font-size: 0;
        }

        #image li {
            display: inline-block;
            width: 50px;
            height: 50px;
            margin-left: 1%;
            padding-top: 1%;
        }

        .viewer-prev, .viewer-next, .viewer-one-to-one, .viewer-title, .viewer-play {
            display: none;
        }


        /*#dowebok li img { width: 200%;}*/
    </style>
</head>
<body>

<ul id="image">
    <#list imgUrls as img>
        <#assign img="${img}">
        <li><img id="${img}" url="${img}" src="${img}" width="1px" height="1px"></li>
    </#list>
</ul>

<script>
    var viewer = new Viewer(document.getElementById('image'), {
        url: 'src',
        navbar: false,
        button: false,
        backdrop: false,
        loop: true,
        prev: false,
        next: false,
    });
    document.getElementById("${currentUrl}").click();

    /*初始化水印*/
    window.onload = function () {
        initWaterMark();
    }
</script>
</body>

</html>
