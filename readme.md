# Convert-Preview 文件格式转换及预览服务

本服务支持：

将常见的文档类文件转换为Pdf、Ofd文件，也可转换为Jpg格式图片；支持对文档类文件的在线预览。

将常见的视频文件转换为MP4、音频文件转换为MP3；支持对音视频文件转换后进行在线播放；支持对视频文件加入水印、截取指定时间的画面等功能。

支持通过path、url方式获取源文件；

支持对文件加图片水印、文字水印；

支持path、ftp、url方式回写文件。

- 本服务支持的文档类文件输入格式为：
  
  - 图片格式：tif、png、jpg、bmp、psd、sgi、pcx、webp、batik、icns、pnm、pict、tga、iff、hdr、gif
  
  - Office系列：
    
    - Word：doc、docx
    
    - Excel：xls、xlsx
    
    - PowerPoint：ppt、pptx
    
    - Visio：vsd、vsdx
  
  - OpenOffice系列：odt、odp、ods
  
  - WPS系列：wps
  
  - XML：支持文本化转换、预览；支持数电票、xbrl等格式化xml套版转换、预览。
  
  - 其他：csv、tsv

- 本服务支持的音视频文件格式为：avi、mp4、mkv、mov、flv、webm、mp3、m4a、ogg、3gp等（FFmpeg支持）。

---

## 特性

* 支持多种文件输入方式：文件路径、http（get）下载、ftp，可扩展
* 支持输入多种文件格式。
* 文档类文件支持水印：文字水印、图片水印、归档章、页码；视频文件支持水印：文字水印、图片水印。
* 文档类文件支持生成双层PDF；视频文件、音频文件支持设置转换编码、分辨率等参数。
* 支持多种文件回写方式：文件路径（path）、http协议上传（url）、FTP服务上传（ftp）、Ecology接口回写（ecology），可扩展。
* 支持转换结果回调。
* 支持失败延迟重试。
* PDF文件支持涂抹遮盖（依赖E9前端功能）。
* 支持契约锁电子签验证（不可见签章）；支持私有签章签署PDF、OFD文件。

## 依赖

* `jdk8`: 编译、运行环境
* `转换引擎`：
  * `WPS预览服务`：支持Linux系统，需要8c32g，系统盘不小于100G，数据盘XFS格式，操作系统CentOS 7.9。
  * `LibreOffice`：支持Windows、Linux环境，但是对Word文档兼容性不好，容易跑版。不推荐。
  * `WPS应用`：Windows版本地应用程序，速度快，版式兼容性好。推荐。
  * `Office应用`：Windows版本地应用程序，速度较慢，单线程。对WPS生成的文档格式兼容性不好，会跑版。且生成PDF无法使用在线的pdf.js预览。谨慎使用。
* `maven`: 编译打包，只运行`jar`不需要，建议`V3.6.3`以上版本
* `rabbitMQ`: 重试机制依赖MQ延迟队列，需安装插件 `rabbitmq_delayed_message_exchange`

## 快速启动

1. 获取`jar`包：联系档案项目组或使用`mvn clean package -Dmaven.test.skip=true`编译。

2. 获取`license`文件：联系档案项目组获取, 试用则不需要，可免费试用7天。

3. 修改配置`application.yml`：
   
   1. 接收的输入文件存储的临时文件夹:`convert.path.inPutTempPath`:
   
   > Windows： D:/work/input/
   > 
   > Linux： /work/input/
   
   2. 默认的本地输出文件路径: `convert.path.outPutPath`:
   
   > Windows： D:/work/output/
   > 
   > Linux： /work/output/
   
   3. 如需支持失败重试功能，需配置 RabbitMQ 相关参数  

4. 确认文件目录结构

```
│  application.yml               配置文件
│  convertpreview-{版本号}.jar    运行jar
│  {项目名}.license               license文件，文件名不重要
│  utils                         工具程序文件夹
│  watermark                     水印、归档章文件夹
```

5. 以管理员身份运行
   
   > Windows： javaw -jar convertpreview-{版本号}.jar
   > 
   > Linux： nohup java -jar convertpreview-{版本号}.jar &

6. 浏览器访问 `http://{ip}:{端口}` , 返回 **启动成功** 标识项目启动正常

## 常见问题

1. 项目日志在哪里？
   
   运行目录下log文件夹内

2. 项目启动失败，日志中有`The Tomcat connector configured to listen on port 8080 failed to start. The port may already be in use or the connector may be misconfigured.`的报错
   
   端口被占用，修改`application.yml`中`server.port`, 改为其他端口

---

# 详细说明

## 配置说明

[配置说明](./docs/配置说明.md)

---

## 转换接口使用说明

本服务提供REST接口供外部系统调用，提供了直接转换接口和通过MQ异步转换的接口。

### 文件格式转换接口说明

[文件格式转换接口说明](./docs/文件格式转换接口说明.md)

### 生成文件后返回Base64字符串

[生成文件后返回Base64字符串](./docs/生成文件后返回Base64字符串.md)

### 生成文件后返回文件流

[生成文件后返回文件流](./docs/生成文件后返回文件流.md)

---

## 预览接口使用说明

### 预览接口（页面）说明

[预览接口（页面）说明](./docs/预览接口（页面）说明.md)

### PDF脱敏（涂抹）说明

[PDF脱敏（涂抹）说明](./docs/PDF脱敏（涂抹）说明.md)
