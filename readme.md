**ConvertOffice Service** 

**Office格式文件转换PDF/OFD服务**

# 简介

本服务用于将常见的文档转换为PDF或OFD文件。支持path、url方式获取源文件；支持对文件加图片水印、文字水印；支持path、ftp、url方式回写文件。

本服务支持的输入文件格式为：Office系列（doc、docx、xls、xlsx、ppt、pptx）、OpenOffice系列（odt、odp、ods）、WPS系列（wps）、csv、tsv、ofd（转pdf）。

转换后输出格式为：PDF、OFD。

# 配置说明

## LibreOffice配置

本服务依赖LibreOffice将各类文档转换为PDF。

使用本服务前，需要现在服务器端安装LibreOffice。

可以访问  [下载 LibreOffice | LibreOffice 简体中文官方网站 - 自由免费的办公套件](https://zh-cn.libreoffice.org/download/libreoffice/)   下载对应版本的软件。

### Windows环境安装

Windows64位版下载地址：https://www.libreoffice.org/donate/dl/win-x86_64/7.2.4/zh-CN/LibreOffice_7.2.4_Win_x64.msi

下载后，运行安装程序，安装到本地文件夹即可。



### Linux环境安装和卸载

Linux系统下的安装包请到下载页面获取。有两种打包方式：适用于 Debian/Ubuntu 的 ".deb" 包，以及适用于 Fedora/SUSE/Mandriva 的 ".rpm" 包。请选择适合您的操作系统的类型。

Linux64位（deb）下载地址：https://www.libreoffice.org/donate/dl/deb-x86_64/7.2.4/zh-CN/LibreOffice_7.2.4_Linux_x86-64_deb.tar.gz

Linux64位（rpm）下载地址：https://www.libreoffice.org/donate/dl/rpm-x86_64/7.2.4/zh-CN/LibreOffice_7.2.4_Linux_x86-64_rpm.tar.gz

#### 安装

##### Debian/Ubuntu系统 (.deb包):

```
# 切换到安装包所在的目录$ cd ~/下载/# 安装主安装程序的所有deb包
$ sudo dpkg -i ./LibreOffice_X.Y.Z_Linux_x86_deb/DEBS/*.deb
# 安装中文语言包中的所有deb包 */
$ sudo dpkg -i ./LibreOffice_4.x.x_Linux_x86_deb_langpack_zh-CN/DEBS/*.deb
/* 安装中文离线帮助文件中的所有deb包 */
$ sudo dpkg -i ./LibreOffice_4.x.x_Linux_x86_deb_helppack_zh-CN/DEBS/*.deb
```

##### Fedora/SUSE/Mandriva系统 (.rpm包):

```
$ cd ~/下载/ /* 切换到安装包所在的目录 *
$ sudo yum install ./LibreOffice_4.x.x_Linux_x86_rpm/RPMS/*.rpm  /* 安装主安装程序的所有rpm包 */
$ sudo yum install ./LibreOffice_4.x.x_Linux_x86_rpm_langpack_zh-CN/RPMS/*.rpm  /* 安装中文语言包中的所有rpm包 */
$ sudo yum install ./LibreOffice_4.x.x_Linux_x86_rpm_helppack_zh-CN/RPMS/*.rpm  /* 安装中文离线帮助文件中的所有rpm包 */
```

#### 卸载

##### Debian/Ubuntu系统 (.deb包):

```
$ sudo apt-get remove --purge libreoffice4.x-*  /* 移除所有类似libreoffice4.x-*的包。--purge表示卸载的同时移除所有相关的配置文件 */
```

##### Fedora/SUSE/Mandriva系统 (.rpm包):

```
$ sudo yum remove libreoffice4.x-*  /* 移除所有类似libreoffice4.x-*的包。 */
```



## 微服务配置

本服务的所有配置信息均在于jar包同级文件夹中的application.yml中，默认内容如下：

```yml
# Tomcat
server:
  tomcat:
    uri-encoding: UTF-8
    max-threads: 1000
    min-spare-threads: 30
  # 端口号
  port: 8080
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
    name: com.thinkdifferent.convertoffice
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
  # 配置文件名
  config: log4j2.xml
  level:
    com.thinkdifferent: trace

# 线程设置参数 #######
ThreadPool:
  # 核心线程数10：线程池创建时候初始化的线程数
  CorePoolSize: 10
  # 最大线程数20：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
  MaxPoolSize: 20
  # 缓冲队列200：用来缓冲执行任务的队列
  QueueCapacity: 200
  # 保持活动时间60秒
  KeepAliveSeconds: 60
  # 允许线程的空闲时间60秒：当超过了核心线程出之外的线程在空闲时间到达之后会被销毁
  AwaitTerminationSeconds: 60

jodconverter:
  local:
    enabled: true
    # libreOffice根目录
    office-home: D:/LibreOffice
    # 端口（线程）
    portNumbers: [2001,2002,2003]
    # LibreOffice进程重启前的最大进程数
    maxTasksPerProcess: 100
    # 任务执行的超时时间
    taskExecutionTimeout: 86400000
    # 任务队列的超时时间
    taskQueueTimeout: 86400000
    # 一个进程的超时时间
    processTimeout: 86400000


# 本服务设置
convert:
  office:
    # 接收的输入文件存储的临时文件夹
    inPutTempPath: D:/cvtest/temp/
    # 默认本地输出文件所在文件夹
    outPutPath: D:/cvtest/
  watermark:
    text:
      # 水印模式：static，静态；dynamic，动态
      type: static
      # 当水印模式为static时，需要设置此项，为固定输出的水印文字
      context: 我的公司
```

可以根据服务器的实际情况进行修改。

重点需要修改的内容：

- Nacos服务设置：设置是否启用、服务地址和端口。
- 线程参数设置：需要根据实际硬件的承载能力，调整线程池的大小。
- RabbitMQ设置：根据实际软件部署情况，控制是否启用RabbitMQ；如果启用RabbitMQ，一定要根据服务的配置情况修改地址、端口、用户名、密码等信息。
- jodconverter设置：重点修改“office-home”的值，**一定要写LibreOffice在本服务器中安装的路径**。
- 本服务设置：根据本服务所在服务器的实际情况，修改本地文件输出路径。

# 使用说明

本服务提供REST接口供外部系统调用，提供了直接转换接口和通过MQ异步转换的接口。

## 图片转换接口说明

直接生成JPG/PDF接口URL：http://host:port/api/convert

MQ异步生成JPG/PDF接口URL：http://host:port/api/convert4mq

接口调用方式：POST

传入参数形式：JSON

传入参数示例：

```JSON
{
	"inputType": "path",
	"inputFile": "D:/1.docx",
	"outPutFileName": "1-online",
	"outPutFileType": "ofd",
	"waterMark": {
		"waterMarkType": "text",
		"waterMarkText": "内部文件",
		"degree": "45",
		"alpha": "0.5f",
		"fontName": "宋体",
		"fontSize": "20",
		"fontColor": "gray"
	},
	"writeBackType": "path",
	"writeBack": {
		"path": "D:/cvtest/"
	},
	"callBackURL": "http://10.11.12.13/callback"
}
```

以下分块解释传入参数每部分的内容。

### 输入信息

系统支持本地文件路径输入（path）和http协议的url文件下载输入（url）。

当使用文件路径输入时，配置示例如下：

```json
	"inputType": "path",
	"inputFile": "D:/1.docx",
```

- inputType：必填，值为“path”。
- inputFile：必填，值为需转换的图片文件（输入文件）在当前服务器中的路径和文件名。（如果输入的文件是ofd，则支持输出转换为pdf）

当使用url文件下载输入时，配置示例如下：

```json
	"inputType": "url",
	"inputFile": "http://localhost/file/1.docx",
```

- inputType：必填，值为“url”。
- inputFile：必填，值为需转换的图片文件（输入文件）在Web服务中的URL地址。（如果输入的文件是ofd，则支持输出转换为pdf）

### 水印设置

系统支持在转换的PDF/OFD文件中加入图片水印或文字水印。

如果需要加入图片水印，设置如下：

```json
    "waterMark":{
        "waterMarkType":"pic",
        "waterMarkFile":"watermark.png",
        "alpha":"0.5f",
        "LocateX":"150",
        "LocateY":"150",
        "waterMarkWidth":"100",
        "waterMarkHeight":"100"
    },
```

- waterMarkType：必填，图片水印为“pic”。
- waterMarkFile：必填，为本服务“watermark”文件夹中已经存放的水印文件。建议使用png格式的文件（支持半透明）。
- alpha：非必填，透明度。默认值“0.5f”。浮点小数，添加的值必须以“f”结尾。
- LocateX：非必填，水印在文档中横轴的位置。默认值为宽度的1/6位置（左下角）。
- LocateY：非必填，水印在文档中纵轴的位置。默认值为高度的1/6位置（左下角）。
- waterMarkWidth：非必填，水印图片的宽度。默认值为150。
- waterMarkHeight：非必填，水印图片的高度。默认值为150。

如果需要加入文字水印，设置如下：

```json
	"waterMark": {
		"waterMarkType": "text",
		"waterMarkText": "内部文件",
		"degree": "45",
		"alpha": "0.5f",
		"fontName": "宋体",
		"fontSize": "20",
		"fontColor": "gray"
	},
```

- waterMarkType：必填，文字水印为“text”。
- waterMarkText：必填，水印的文字内容。
- degree：必填，旋转角度。
- alpha：非必填，透明度。默认值“0.5f”。浮点小数，添加的值必须以“f”结尾。
- fontName：非必填，字体名称。
  - 如果输出格式为“pdf”，则此处默认值为“宋体”。此处的字体名称为itext包中已有的字体。
  - 如果输出格式为“ofd”，则此处默认值为"STSONG.TTF"。此处的字体名称为系统font文件夹中的字体文件名，后续可自行添加扩展。

- fontSize：非必填，字号大小。默认值40。
- fontColor：非必填，字体颜色。默认值“gray”（灰色）。

### 输出信息

可以设置输出的Pdf/Ofd文件的文件名（无扩展名）和输出的文件类型，如下：

```json
	"outPutFileName": "1-online",
	"outPutFileType": "pdf",
```

- outPutFileName：必填，为文件生成后的文件名（无扩展名）。
- outPutFileType：必填，为文件生成后的扩展名。本服务支持：pdf、ofd。

本例中，即转换后生成名为 1-online.pdf 的文件。

### 回写信息

本服务支持以下回写方式：文件路径（path）、http协议上传（url）、FTP服务上传（ftp）。

注意：返回Base64接口无此部分回写信息。

当使用文件路径（Path）方式回写时，配置如下：

```json
	"writeBackType": "path",
	"writeBack": {
		"path": "D:/cvtest/"
	},
```

- writeBackType：必填，值为“path”。
- writeBack：必填。JSON对象，path方式中，key为“path”，value为文件回写的路径。

当使用http协议上传（url）方式回写时，配置如下：

```json
	"writeBackType": "url",
	"writeBack": {
		"url": "http://localhost/uploadfile.do"
	},
	"writeBackHeaders": {
		"Authorization": "Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
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
		"Authorization": "Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0"
	},
```

- callBackURL：回调接口的URL。回调接口需要接收两个参数：
  - file：处理后的文件名。本例为“1-online”。
  - flag：处理后的状态，值为：success 或 error。
- callBackHeaders：如果回调接口需要在请求头中加入认证信息等，可以在此处设置请求头的参数和值。

接口url示例：

```
http://10.11.12.13/callback.do?file=1-online&flag=success
```

### 返回信息

接口返回信息示例如下：

```json
{
  "flag": "success",
  "message": "Convert Office to PDF success."
}
```

- flag：处理状态。success，成功；error，错误，失败。
- message：返回接口消息。



## 生成PDF后转换为Base64字符串返回

生成一个PDF后返回Base64字符串接口URL：http://host:port/api/convert2base64

生成多个PDF后返回Base64字符串接口URL：http://host:port/api/convert2base64s

接口调用方式：POST

传入参数形式：JSON

传入参数示例：

```JSON
{
	"inputType": "path",
	"inputFile": "D:/cvtest/1.docx",
	"outPutFileName": "1-online",
	"outPutFileType": "pdf"
}
```

以下分块解释传入参数每部分的内容。

### 输入信息

系统支持本地文件路径输入（path）和http协议的url文件下载输入（url）。

配置示例请见上述章节。

### 输出信息

可以设置输出的Pdf/Ofd文件的文件名（无扩展名）和输出的文件类型，如下：

```json
	"outPutFileName": "1-online",
	"outPutFileType": "pdf",
```

- outPutFileName：必填，为文件生成后的文件名（无扩展名）。
- outPutFileType：必填，为文件生成后的扩展名。本接口中，可以设置：pdf、ofd。

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
    "message": "Convert Office to PDF success",
    "base64": [
        {
            "filename": "1-online.pdf",
            "base64": "JVBERi0xLjQKJeLjz9MKNCAwIG9iago8PC9TdWJ0eXBlL0Zvcm0vRmlsdGVy…………"
        },
        {
            "filename": "2-online.pdf",
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

# 代码结构说明

本项目所有代码均在  com.thinkdifferent.convertpic 之下，包含如下内容：

- config
  - ConvertOfficeConfig：本服务自有配置读取。
  - RabbitMQConfig：RabbitMQ服务配置读取。
- consumer
  - ConvertOfficeConsumer：MQ消费者，消费队列中传入的JSON参数，执行任务（Task）。
- controller
  - ConvertOffice：REST接口，提供直接转换Jpg/Pdf接口，和调用MQ异步生成PDF/OFD的接口。
- service
  - ConvertOfficeService：文档转换为Pdf/Ofd文件、文件回写上传、接口回调等核心逻辑处理。
  - RabbitMQService：将JSON消息加入到队列中的服务层处理。
- task
  - Task：异步多线程任务，供MQ消费者调用，最大限度的提升并行能力。
- utils
  - ConvertOfficeUtil：文档转换处理的工具类，可以将传入的图片转换为Pdf或Ofd。
  - SpringUtil：获取服务实例工具类。
  - WaterMarkUtil：水印处理工具类，支持图片水印、文字水印。
  - WriteBackUtil：回写文件、回调接口的工具类。



