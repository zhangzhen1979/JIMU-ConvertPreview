**ConvertPreview Service**

[![快速开始](https://img.shields.io/badge/%E8%AF%95%E7%94%A8-%E5%BF%AB%E9%80%9F%E5%BC%80%E5%A7%8B-blue.svg)](readme.md)
[![详细介绍](https://img.shields.io/badge/%E6%8E%A5%E5%8F%A3-%E8%AF%A6%E7%BB%86%E4%BB%8B%E7%BB%8D-blue.svg)](detail.md)

**文件格式转换（PDF/OFD/JPG）及预览服务**

# 简介

本服务用于将常见的文件转换为Pdf、Ofd文件，也可转换为Jpg格式图片。支持path、url方式获取源文件；支持对文件加图片水印、文字水印；支持path、ftp、url方式回写文件。

本服务支持的输入格式为：

- 图片格式：tif、png、jpg、bmp、psd、sgi、pcx、webp、batik、icns、pnm、pict、tga、iff、hdr、gif

- Office系列：
  
  - Word：doc、docx
  
  - Excel：xls、xlsx
  
  - PowerPoint：ppt、pptx
  
  - Visio：vsd、vsdx

- OpenOffice系列：odt、odp、ods

- WPS系列：wps

- 其他：csv、tsv

转换后输出格式为：PDF、OFD、JPG。



# 配置说明

## 转换引擎

本系统支持转换引擎包括如下内容：（必须选其一，否则无法完成文档格式转换）

- WPS本地软件（推荐Jacob方式单线程调用）。推荐。需要Windows环境部署。（支持Word、Excel、PowerPoint格式转换，速度快）

- Office本地软件（推荐Jacob方式单线程调用）。需要Windows环境部署。（支持Word、Excel、PowerPoint、Visio格式转换，速度慢，PDF格式兼容性不好）

- LibreOffice本地软件。支持Linux、Windows环境部署。（Word跑版，不推荐）

## 配置文件

本服务的所有配置信息均在于jar包同级文件夹中的application.yml中，默认内容如下：

```yml
# Tomcat
server:
  # 端口号
  port: 8080
  tomcat:
    uri-encoding: UTF-8
    threads:
      max: 1000
      min-spare: 30
    # 超时时间
    connection-timeout: 5000

spring:
  # RabbitMQ设置
  rabbitmq:
    # 访问地址
    host: 127.0.0.1
    # 端口
    port: 5672
    # 用户名
    username: guest
    # 密码
    password: guest
    # 监听设置
    listener:
      # 生产者
      direct:
        # 自动启动开关
        auto-startup: false
      # 消费者
      simple:
        # 自动启动开关
        auto-startup: false

  application:
    # 应用名称。如果启用nacos，此值必填
    name: convert
  cloud:
    # Nacos的配置。
    # 如果启用Nacos服务作为配置中心，
    # 则此部分之后的内容均可以在Nacos配置中心中管理，
    # 不必在此配置文件中维护。
    nacos:
      config:
        # 配置服务地址
        server-addr: 127.0.0.1:8848
        # 启用状态
        enabled: false
      discovery:
        # 服务发现服务地址
        server-addr: 127.0.0.1:8848
        # 启用状态
        enabled: false


# log4j2设置
logging:
  level:
    root: info
    com.thinkdifferent: debug
    de.codecentric.boot.admin.client: error
  file:
    name: logs/application.log

# 本服务设置
convert:
  preview:
    # 默认文件预览方式：pdf | img， 默认 img
    type: img
    # 是否显示JPG/PDF切换按钮: true | false, 默认 true
    blnChange: false
    # 图片水印地址, 优先级比文字水印高
    #    watermarkImage: watermark/watermark.png
    # 文字水印内容
    watermarkTxt: 文字水印

  # 转换重试功能需启用MQ才有效
  retry:
    # 最大重试次数（0-8）, 0标识不重试, 若异常情况只记录日志， 大于1（最大8）：标识失败进行重试的次数, 将会在以下时间重试（5min, 10min, 30min, 1h, 2h, 4h, 8h, 16h）
    max: 3
  pdf:
    # pdf 用字体文件路径 仿宋
    font: C:/Windows/Fonts/simfang.ttf
  path:
    # 接收的输入文件存储的临时文件夹
    inPutTempPath: D:/cvtest/input/
    # 默认本地输出文件所在文件夹
    outPutPath: D:/cvtest/output/
    # 清理临时目录的间隔时间，单位：天（正整数）, 默认：1
    cleanUnit: 1

  # 转换引擎配置
  engine:
    # 可转换的图片格式
    picType: tif,tiff,png,jpg,jpeg,bmp,psd,sgi,pcx,webp,batik,icns,pnm,pict,tga,iff,hdr,gif
  g
localUtil:
  # 使用本地WPS应用转换
  wps:
    # 是否启用
    enabled: true
    # 文件格式
    fileType: txt,csv,doc,docx,xls,xlsx,ppt,pptx,rtf
    # 运行类型：exe/jacob。推荐jacob（单线程）
    runType: jacob

  # 使用本地Office应用转换的文件格式。
  office:
    # 是否启用
    enabled: false
    # 文件格式(比WPS多支持Visio文件格式)
    fileType: txt,csv,doc,docx,xls,xlsx,ppt,pptx,vsd,vsdx
    # 运行类型：exe/jacob。推荐jacob（单线程）
    runType: jacob

jodconverter:
  local:
    # 是否启用LibrOffice引擎。使用其他引擎时，此项必须设置为false
    enabled: false
    # libreOffice根目录
    office-home: D:/LibreOffice
    # 端口（线程）
    portNumbers: 8001,8002,8003
    # LibreOffice进程重启前的最大进程数
    maxTasksPerProcess: 100
    # 任务执行的超时时间
    taskExecutionTimeout: 86400000
    # 任务队列的超时时间
    taskQueueTimeout: 86400000
    # 一个进程的超时时间
    processTimeout: 86400000
```

可以根据服务器的实际情况进行修改。

重点需要修改的内容：

- Nacos服务设置：设置是否启用、服务地址和端口。
- RabbitMQ设置：根据实际软件部署情况，控制是否启用RabbitMQ；如果启用RabbitMQ，一定要根据服务的配置情况修改地址、端口、用户名、密码等信息。
- 本服务设置：根据本服务所在服务器的实际情况，修改本地文件输出路径。
- 重试机制： 依赖RabbitMQ, 只有在RabbitMQ启动得到情况下才会生效
- jodconverter设置：重点修改“office-home”的值，**一定要写LibreOffice在本服务器中安装的路径**。



# 使用说明

本服务提供REST接口供外部系统调用，提供了直接转换接口和通过MQ异步转换的接口。

## 转换接口说明

生成JPG/PDF/OFD接口URL：http://host:port/api/convert

接口调用方式：POST

传入参数形式：JSON

传入参数示例：

```json
{
  "inputType": "path",
  "inputFile": "D:/cvtest/001.tif",
  "outPutFileName": "001-online",
  "outPutFileType": "jpg",
  "outEncry": {
    "username": "zhang3",
    "userPassword": "zhang3pwd",
    "ownerPassword": "ownerpwd",
    "copy": false,
    "modify": false,
    "print": false
  },
  "waterMark": {
    "pic": {
      "waterMarkFile": "watermark.png",
      "LocateX": "40",
      "LocateY": "10",
      "imageWidth": "50",
      "imageHeight": "50"
    },
    "text": {
      "waterMarkText": "办公室 普通用户||192.168.1.1||2022-10-14 12:01:01",
      "degree": "45",
      "fontName": "宋体",
      "fontSize": "20",
      "fontColor": "gray"
    },
    "alpha": "0.5f",
    "pageNum": true
  },
  "firstPageMark": {
    "base64": "adffadsr2r234234234234234=",
    "template": "gdz.html",
    "pngWidth": 810,
    "pngHeight": 250,
    "locate": "TR",
    "data": {
      "tableWidth": "800px",
      "tdWidth": "200px",
      "tdHeight": "120px",
      "fontSize": "40px",
      "fonds_no": "1001",
      "year": "2001",
      "piece_no": "123",
      "department": "办公室",
      "retention": "长期",
      "page_amounts": "10"
    }
  },
  "writeBackType": "path",
  "writeBack": {
    "path": "D:/cvtest/"
  }
}
```

以下分块解释传入参数每部分的内容。

### 输入信息

系统支持本地文件路径输入（path）、http协议的url文件下载输入（url）、ftp服务路径输入（ftp）。可以传入各种文档格式，由系统转换为PDF/OFD；也可以直接传入PDF/OFD，由系统自动进行后续的添加水印、添加归档章的操作。

当使用文件路径输入时，配置示例如下：

```json
"inputType": "path",
"inputFile": "D:/cvtest/001.tif",
```

- inputType：必填，值为“path”。
- inputFile：必填，值为需转换的图片文件（输入文件）在当前服务器中的路径和文件名。

当使用url文件下载输入时，配置示例如下：

```json
"inputType": "url",
"inputFile": "http://localhost/file/1.tiff",
"inputFileType": "tiff",
```

- inputType：必填，值为“url”。
- inputFile：必填，值为需转换的文件（输入文件）在Web服务中的URL地址。
- inputFileType：“inputType”为“url”时，必填。值为url链接的文件的扩展名。

当使用`ftp`时，配置示例如下：

```json
"inputType": "ftp",
// 有密码配置
"inputFile": "ftp://ftptest:zx@192.168.0.102/archives/ftptest/tjTest/test.png"
// 无密码配置
// "inputFile":"ftp://192.168.0.102/archives/ftptest/tjTest/test.doc"
```

- inputType：必填，值为“ftp”。
- inputFile：必填，值为需转换的图片文件（输入文件）在FTP服务中的地址,兼容用户密码。

### 多文件合并转换

系统支持将多个JPG文件合并为一个PDF/OFD文件；或PDF文件合并为一个PDF文件；或多个OFD文件合并为一个OFD文件。

当使用文件路径输入时，配置示例如下：

```json
"inputType": "path",
"inputFiles": [
    {
        "inputFile": "D:/cvtest/001.pdf",
        "inputFileType": "pdf"
    },
    {
        "inputFile": "D:/cvtest/002.pdf",
        "inputFileType": "pdf"
    }
],
"outPutFileName": "001-online",
```

url、ftp方式配置内容与【输入信息】章节中说明一致。

系统会按照PDF文件传入的顺序，将其合并为一个新的PDF文件。

- inputType：必填，文件输入方式，path、url、ftp
- 
- inputFiles：此场景下必填，输入的多个PDF文件路径。
- outPutFileName：必填，为文件生成后的文件名，扩展名自动为pdf/ofd。

### PDF加密设置

系统支持在转换的PDF文件中设置用户名和密码，并可控制PDF文件的使用权限。

（可选项，不设置则此项不传值即可）

示例如下：

```json
"outEncry": {
    "username": "zhang3",
    "userPassword": "zhang3pwd",
    "ownerPassword": "ownerpwd",
    "copy": false,
    "modify": false,
    "print": false,
    "assembleDocument": false,
    "fillInForm": false,
    "modifyAnnotations": false,
    "printDegraded": false
},
```

- username：用户名，必填。
- userPassword：用户密码（权限受控），必填。
- ownerPassword：拥有者密码（不控制权限），必填。
- copy：是否允许用户复制（权限受控），必填。
- modify：是否允许用户编辑（权限受控），必填。
- print：是否允许用户打印（权限受控），必填。
- assembleDocument：是否可以插入/删除/旋转页面，非必填。
- fillInForm：是否可以填写交互式表单字段（包括签名字段），非必填。
- modifyAnnotations：是否可以添加或修改文本注释并填写交互式表单字段，如果canModify()返回true，则创建或修改交互式表单字段（包括签名字段）。非必填。
- printDegraded：是否可以降级格式打印文档，非必填。

### OFD加密设置

系统支持在转换的OFD文件中设置访问密码。建议使用“超越版式办公套件”浏览加密后的OFD（数科阅览器、WPS均不支持打开加密OFD）。

（可选项，不设置则此项不传值即可）

示例如下：

```json
"outEncry": {
    "username": "admin",
    "userPassword": "zhang3pwd",
    "copy": false,
    "modify": false,
    "print": false,
    "copies": 3,
    "signature": false,
    "watermark": false,
    "export": false,
    "modifyAnnotations": false,
    "validPeriodStart": "2022-12-01",
    "validPeriodEnd": "2022-12-31"
},
```

- username：用户名，非必填。为兼容【超越版式办公套件】，OFD文件建议传入固定值admin。如不传入此参数，则系统自动设置用户名为admin。
- userPassword：用户密码（权限受控），使用加密时必填。
- copy：是否允许用户复制（权限受控），必填。
- modify：是否允许用户编辑（权限受控），必填。
- print：是否允许用户打印（权限受控），必填。
- copies：允许打印的份数（允许打印时有效），非必填。
- signature：是否允许添加签章，非必填。
- watermark：是否允许添加水印，非必填。
- export：是否允许导出，非必填。
- modifyAnnotations：是否允许添加批注，非必填。
- validPeriodStart：有效期开始日期。使用有效期设置时必填。
- validPeriodEnd：有效期结束日期。使用有效期设置时必填。

### 水印设置

系统支持在转换的JPG/PDF/OFD文件中加入图片水印或文字水印。

如果需要加入图片水印，设置如下：

```json
"waterMark": {
    "pic": {
        "waterMarkFile": "watermark.png",
        "LocateX":"10",
        "LocateY": "10",
        "imageWidth": "50",
        "imageHeight": "50"
    }
},
```

- waterMarkFile：必填，为本服务“watermark”文件夹中已经存放的水印文件。建议使用png格式的文件（支持半透明）。
- LocateX：非必填，单位：毫米。水印中心点在文档中横轴的位置。默认值为宽度的1/6位置（左下角）。
- LocateY：非必填，单位：毫米。水印中心点在文档中纵轴的位置。默认值为高度的1/6位置（左下角）。
- imageWidth：非必填，单位：毫米。水印图片的宽度。默认值为50。
- imageHeight：非必填，单位：毫米。水印图片的高度。默认值为50。

如果需要加入文字水印，设置如下：

```json
"waterMark": {
    "text": {
        "waterMarkText": "办公室 普通用户||192.168.1.1||2022-10-14 12:01:01",
        "degree": "45",
        "fontSize": "20",
        "fontColor": "gray"
    }
},
```

- waterMarkText：必填，水印的文字内容。支持多行水印，使用“||”分隔符。
- degree：必填，旋转角度。
- fontSize：非必填，字号大小。默认值40。
- fontColor：非必填，字体颜色。默认值“gray”（灰色）。

公共参数，设置如下：

```json
"alpha": "1f",
"pageNum": true
```

- alpha：非必填，透明度，传入文件为PDF/OFD时有效。默认值“1f”。浮点小数，添加的值必须以“f”结尾。如果需要给PDF、OFD文件添加透明水印，则此处透明度设置为“0f”。
- pageNum：非必填，生成页码。true为生成；false或不填写此项，则不生成。



### 输出信息

可以设置输出的Jpg/Pdf文件的文件名（无扩展名）和输出的文件类型，如下：

```json
"outPutFileName": "001-online",
"outPutFileType": "jpg",
```

- outPutFileName：必填，为文件生成后的文件名（无扩展名）。
- outPutFileType：必填，为文件生成后的扩展名。本服务支持：jpg、pdf、ofd

本例中，即转换后生成名为 001-online.jpg 的文件。

### 缩略图设置

如果需要生成缩略图，则需要添加如下内容。（注意：使用【缩略图】设置后，【图片水印】、【文字水印】、【首页水印】、【双层PDF】等设置均失效。只生成缩略图。）

缩略图输出固定使用“jpg”格式。

设定缩略图边长：

```json
"thumbnail": {
    "width": 200,
    "height": 400
},
```

- width：非必填（width和height可只填其一，也可都填），缩略图的宽度像素值。不填写height值，则根据宽度自动按原比例计算高度。
- height：非必填（width和height可只填其一，也可都填），缩略图的高度像素值。不填写width值，则根据高度自动按原比例计算宽度。

或设定缩略图比例：

```json
"thumbnail": {
    "scale": 0.8,
    "quality": 0.9
},
```

- scale：必填，图片缩放比例。

- quality：必填，为缩略图压缩比。
  
  生成缩略图的完整输入示例如下：

```json
{
  "inputType": "path",
  "inputFile": "D:/cvtest/001.tif",
  "outPutFileName": "001-thumbnail",
  "thumbnail": {
    "width": 200,
    "height": 400
  },
  "writeBackType": "path",
  "writeBack": {
    "path": "D:/cvtest/"
  }
}
```

本例中，即转换后生成名为 001-thumbnail.jpg 的缩略图文件。

### 双层PDF

双层PDF，一种具有多层结构的PDF格式文件，上层是文字内容，下层是原始图像，是可以检索的PDF文件。

```json
"context": [
    {
        "pageIndex": 0,
        "text": "第一页内容",
        "rect": {
            "x": 185,
            "y": 78,
            "width": 238,
            "height": 24
        }
    },
    {
        "pageIndex": 1,
        "text": "第二页内容。。。",
        "rect": {
            "x": 204,
            "y": 109,
            "width": 199,
            "height": 25
        }
    }
]
```

- context: 非必填，仅在`outPutFileType=pdf`时有效，存在该项时标识生成双层PDF
- context.pageIndex: 识别结果页序号，从0开始
- context.text: 识别文本结果

### 回写信息

本服务支持以下回写方式：文件路径（path）、服务器端路径（local）、http协议上传（url）、FTP服务上传（ftp）。

注意：返回Base64接口无此部分回写信息。

当使用文件路径（path）方式回写时，配置如下：

```json
"writeBackType": "path",
"writeBack": {
    "path": "D:/data2pdf/"
},
```

- writeBackType：必填，值为“path”。
- writeBack：必填。JSON对象，path方式中，key为“path”，value为MP4文件回写的路径。

当使用服务器端路径（local）方式回写时，配置如下：

```json
"writeBackType": "local",
```

-

writeBackType：必填，值为“local”。此时，系统自动获取配置文件中【convert.path.outPutPath】设置的路径，将转换后的文件存储在“输出文件夹”中；以待前端系统下载。前端系统访问本服务的“/outfile/文件名”url即可下载。例如：http://127.0.0.1:8080/outfile/cs-alpha0.pdf

当使用http协议上传（url）方式回写时，配置如下：

```json
"writeBackType": "url",
"writeBack": {
    "url": "http://localhost/uploadfile.do"
},
"writeBackHeaders": {
    "token": "da3efcbf-0845-4fe3-8aba-ee040be542c0"
},
```

- writeBackType：必填，值为“url”。
- writeBack：必填。JSON对象，url方式中，key为“url”，value为业务系统提供的文件上传接口API地址。
- writeBackHeaders：非必填。如果Web服务器访问时需要设置请求头或Token认证，则需要在此处设置请求头的内容；否则此处可不添加。

当使用FTP服务上传（ftp）方式回写时，配置如下：

```json
"writeBackType": "ftp",
"writeBack": {
    "passive": "false",
    "host": "ftp://localhost",
    "port": "21",
    "username": "guest",
    "password": "guest",
    "filepath": "/2021/10/"
},
```

- writeBackType：必填，值为“ftp”。
- writeBack：必填。JSON对象。
  - passive：是否是被动模式。true/false
  - host：ftp服务的访问地址。
  - port：ftp服务的访问端口。
  - username：ftp服务的用户名。
  - password：ftp服务的密码。
  - filepath：文件所在的路径。



### 回调信息

业务系统可以提供一个GET方式的回调接口，在视频文件转换、回写完毕后，本服务可以调用此接口，传回处理的状态。

注意：返回Base64接口无此部分信息。

```json
"callBackURL": "http://10.11.12.13/callback.do",
"callBackHeaders": {
    "token": "da3efcbf-0845-4fe3-8aba-ee040be542c0"
},
```

- callBackURL：回调接口的URL。回调接口需要接收两个参数：
  - file：处理后的文件名。本例为“001-online.jpg”（如果使用ecology方式回写，则此处回传上传接口返回的id）。
  - flag：处理后的状态，值为：success 或 error。
  - pageNum：转换后文件的页数。
- callBackHeaders：如果回调接口需要在请求头中加入认证信息等，可以在此处设置请求头的参数和值。

接口url示例：

```
http://10.11.12.13/callback.do?file=001-online&flag=success
```

### 返回信息

接口返回信息示例如下：

```json
{
  "flag": "success",
  "message": "Convert Pic to JPG/PDF/OFD success."
}
```

- flag：处理状态。success，成功；error，错误，失败。
- message：返回接口消息。

## 生成文件后转换为Base64字符串返回

生成一个目标文件后返回Base64字符串接口URL：http://host:port/api/convert2base64

生成多个目标文件后返回Base64字符串接口URL：http://host:port/api/convert2base64s

接口调用方式：POST

传入参数形式：JSON

传入参数示例：

```json
{
  "inputType": "path",
  "inputFile": "D:/cvtest/001.tif",
  "outPutFileName": "001-online",
  "outPutFileType": "jpg"
}
```

以下分块解释传入参数每部分的内容。

### 输入信息

系统支持本地文件路径输入（path）、http协议的url文件下载输入（url）、ftp服务路径输入（ftp）。

配置示例请见上述章节。

### 输出信息

可以设置输出的Jpg/Pdf文件的文件名（无扩展名）和输出的文件类型，如下：

```json
"outPutFileName": "001-online",
"outPutFileType": "jpg",
```

- outPutFileName：必填，为文件生成后的文件名（无扩展名）。
- outPutFileType：必填，为文件生成后的扩展名。本接口中，可以设置：jpg、pdf、ofd。

### 返回信息

convert2base64接口返回信息示例如下：

```
JVBERi0xLjQKJeLjz9MKNCAwIG9iago8PC9TdWJ0eXBlL0Zvcm0vRmlsdGVyL0ZsYXRlRGVjb2RlL1R5………………
```

- 返回Base64编码后的Jpg/Pdf文件的内容。可供前端页面直接将其放入iframe的src属性中显示。

convert2base64s接口返回信息示例如下：

```json
{
  "flag": "success",
  "message": "Convert Pic to JPG/PDF/OFD success",
  "base64": [
    {
      "filename": "001-online.jpg",
      "base64": "JVBERi0xLjQKJeLjz9MKNCAwIG9iago8PC9TdWJ0eXBlL0Zvcm0vRmlsdGVy…………"
    },
    {
      "filename": "001-online.pdf",
      "base64": "JVBERi0xLjQKJeLjz9MKNCAwIG9iago8PC9TdWJ0eXBlL0Zvcm0vRmlsdGVy…………"
    }
  ]
}
```

- flag：处理状态。success，成功；error，错误，失败。
- message：返回接口消息。
- base64
  - filename：文件名
  - base64：文件Base64编码之后的字符串。

## 生成文件后返回文件流

生成一个目标文件后返回Http Response文件流：[http://host:port/api/convert2stream](http://host:port/api/convert2stream)

接口调用方式：POST

传入参数形式：JSON

传入参数示例：

```json
{
  "inputType": "path",
  "inputFile": "D:/cvtest/001.tif",
  "outPutFileName": "001-online",
  "outPutFileType": "pdf"
}
```

以下分块解释传入参数每部分的内容。

### 输入信息

系统支持本地文件路径输入（path）、http协议的url文件下载输入（url）、ftp服务路径输入（ftp）。

配置示例请见上述章节。

### 输出信息

可以设置输出的Jpg/Pdf/Ofd文件的文件名（无扩展名）和输出的文件类型，如下：

```json
"outPutFileName": "001-online",
"outPutFileType": "jpg",
```

- outPutFileName：必填，为文件生成后的文件名（无扩展名）。
- outPutFileType：必填，为文件生成后的扩展名。本接口中，可以设置：jpg、pdf、ofd。

### 返回信息

convert2stream接口将转换后的文件输出到Http响应信息中，以文件流方式返回。



## 预览接口（页面）说明

请求地址：/api/onlinePreview

请求方式：GET

请求参数：

- filePath: base64格式后的文件路径。支持以下方式传入文件路径
  - 本地文件：C:/a.doc
  - http链接：http://ip:port/download/fileId
    - md5: 非必须，用于区分文件是否是同一个，优先级较高
    - uuid: 非必须，用于区分文件是否是同一个，优先级较低
  - ftp路径：ftp://username:password@ip:port/dir/file.doc
- fileType: 文件类型，即文件的扩展名。
- keyword：检索词。如果使用pdf方式预览，可以支持单个词高亮显示。
- watermark：base64格式后的水印信息。json格式，支持以下配置
  - content: 文字水印内容，优先级比application.yml配置要高
  - rotate: 旋转角度，默认30
  - opacity: 透明度，默认0.8
  - fontsize: 字体大小，默认18
  - color： 文字水印颜色，默认灰色，#cfcfcf

请求结果：html页面，需通过浏览器进行预览。

请求示例：
http://localhost:8080/api/onlinePreview?filePath=RTpcdGVtcFxvdXRwdXRcMjAyMi0xMi0yOFzmiqXooajmnI3liqEuemlwXOaKpeihqOacjeWKoVzjgJDmlofkuablrprjgJHmiqXooajmnI3liqHpg6jnvbLphY3nva7or7TmmI4tMjAyMi4xMTE1LTIwMjIxMjA1LmRvY3g=&fileType=doc&keyword=2022
