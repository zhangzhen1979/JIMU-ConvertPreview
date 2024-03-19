<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <title>IMG预览文档</title>
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
            /*height: 100%;*/
        }

        .my-photo {
            width: 80%;
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
    <img id="zoomin" src="/images/zoomin.png" width="63" height="63" style="position: fixed; cursor: pointer; top: 55%;right: 48px;
     z-index: 999;height: auto" alt="放大" title="放大" onclick="changeSize(0.1)"/>
    <img src="/images/zoomout.png" width="63" height="63" style="position: fixed; cursor: pointer; top: 65%;right: 48px;
     z-index: 999;height: auto" alt="缩小" title="缩小" onclick="changeSize(-0.1)"/>
    <img src="/images/refresh.png" width="63" height="63" style="position: fixed; cursor: pointer; top: 75%;right: 48px;
     z-index: 999;height: auto" alt="还原" title="还原" onclick="changeSize(0)"/>
</div>
<script>
    document.body.parentNode.style.overflowX = "hidden"
    let checkImgInterval;
    window.onload = function () {
        /*初始化水印*/
        initWaterMark();
        checkImgs();
        initWaterMark();
        asyncLoadImg();
    };
    // window.onload = function (){
    //     initWaterMark();
    //
    // };
    window.onscroll = throttle(checkImgs);

    function changePreviewType(previewType) {
        if (checkImgInterval) {
            clearInterval(checkImgInterval);
        }
        var url = window.location.href;

        if (url.indexOf("outType=officePicture") !== -1) {
            // 加入判断，如果传入的是ofd文件，则切换回OFD预览
            url = url.replace("outType=officePicture", "");
            // 其他，则切换回PDF预览
        } else {
            url = url + "&outType=pdf";
        }
        if ('allImages' === previewType) {
            window.open(url)
        } else {
            window.location.href = url;
        }
    }

    let defaultSize = 0.8

    function changeSize(num) {
        if (num === 0) {
            defaultSize = 0.8
        } else {
            defaultSize = defaultSize + num;
        }
        if (defaultSize <= 0.2) {
            defaultSize = 0.2
        }
        Array.prototype.forEach.call(document.getElementsByClassName('my-photo'), function (element) {
            element.style.width = defaultSize * 100 + '%'
        });
    }


    function getQueryString(name) {
        var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
        // 获取url中"?"符后的字符串并正则匹配
        var r = window.location.search.substr(1).match(reg);
        var context = "";
        if (r != null)
            context = decodeURIComponent(r[2]);
        reg = null;
        r = null;
        return context == null || context === "" || context === "undefined" ? "" : context;
    }

    // todo 定时加载剩余图片，加载完成后不再请求
    function asyncLoadImg() {
        let pdfNum = ${numberOfPages};
        checkImgInterval = setInterval(function () {
                let filePath = "${filePath}";
                $.get("/api/checkImg?filePath=" + filePath,
                    function (data, status) {
                        // jquery 获取 img 数量
                        let nowNum = $("img.my-photo").length;
                        for (let i = nowNum; i < data.length; i++) {
                            let imgData = "<div class='img-area'><img class='my-photo' alt='loading' data-src='"
                                + data[i] +
                                "' src='/images/loading.gif'> </div> <br/>"
                            $("#zoomin").before(imgData);
                        }
                        checkImgs()
                        if (data.length === 0 || data.length === pdfNum) {
                            clearInterval(checkImgInterval);
                        }
                    });
            }
            , 3000);
    }

</script>
</body>
</html>