<!DOCTYPE html>
<html lang="zh-cn">
<head>
    <meta charset="utf-8"/>
    <title>多媒体文件预览</title>
    <#include "*/commonHeader.ftl">
    ${hlsJs}
    <link type="text/css" rel="stylesheet" href="/ckplayer/css/ckplayer.css" />
    <script type="text/javascript" src="/ckplayer/js/ckplayer.js" charset="UTF-8"></script>

    <style>
        * {
            margin:0;
            padding:0;
        }
        .video{
            position:absolute;
            width:100%;
            height:100%;
            background-color:blue;
        }
    </style>
</head>
<body>

<div class="video">媒体播放器</div>
<script type="text/javascript">
    //定义一个变量：videoObject，用来做为视频初始化配置
    var videoObject = {
        container: '.video', //“#”代表容器的ID，“.”或“”代表容器的class
        ${hlsPlug}
        video: '${mediaUrl}'//视频地址
    };
    var player = new ckplayer(videoObject);//初始化播放器

    window.onload = function () {
        /*初始化水印*/
        initWaterMark();
    };
</script>

</body>
</html>
