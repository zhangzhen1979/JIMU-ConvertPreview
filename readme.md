# Convert-Preview 文档格式转换（PDF/OFD/JPG）及预览服务

文档格式转换：本服务用于将常见的文件转换为Pdf、Ofd文件，也可转换为Jpg格式图片。支持path、url方式获取源文件；支持对文件加图片水印、文字水印；支持path、ftp、url方式回写文件。

在线预览（开发中，逐步更新）：支持以下格式文件的在线预览；支持以PDF或JPG方式在线预览；支持预览页面中加入文字水印、图片水印。

本服务支持的输入格式为：

- 图片格式：BMP、GIF、FlashPix、JPEG、PNG、PMN、TIFF、WBMP

- Office系列：doc、docx、xls、xlsx、ppt、pptx

- OpenOffice系列：odt、odp、ods

- WPS系列：wps

- 其他：csv、tsv

转换后输出格式为：PDF、OFD、JPG。

---

## 说明文档

[![快速开始](https://img.shields.io/badge/%E8%AF%95%E7%94%A8-%E5%BF%AB%E9%80%9F%E5%BC%80%E5%A7%8B-blue.svg)](readme.md)
[![详细介绍](https://img.shields.io/badge/%E6%8E%A5%E5%8F%A3-%E8%AF%A6%E7%BB%86%E4%BB%8B%E7%BB%8D-blue.svg)](detail.md)

---

## 感谢

- ofdrw：本项目中OFD转换、处理，均使用ofdrw项目实现。项目地址：[GitHub - ofdrw/ofdrw: OFD Reader &amp; Writer 开源的OFD处理库，支持文档生成、数字签名、文档保护、文档合并、转换等功能，文档格式遵循《GB/T 33190-2016 电子文件存储与交换格式版式文档》。](https://github.com/ofdrw/ofdrw)
- kkFileView：本项目中前端预览界面，均使用kkFileView项目中的页面修改而来，在此特别感谢。项目地址：[kekingcn/kkFileView: 基于 Spring Boot 的文件在线预览项目 (github.com)](https://github.com/kekingcn/kkFileView)
- WpsToPDF：本项目以exe命令行方式调用本地WPS能力提供者。功能简洁，速度飞快。项目地址：[lisisong/wps2pdf: wps doc转pdf的命令行程序 (github.com)](https://github.com/lisisong/wps2pdf)
- OfficeToPDF：本项目以exe命令行方式调用本地Office组件能力提供者。功能极其强大，可以支持调用本地Office组件转换所有Office格式的文件，且格式完美，而且还支持很多命令行参数。项目地址：[Release OfficeToPDF 1.9 · cognidox/OfficeToPDF (github.com)](https://github.com/cognidox/OfficeToPDF)

## 特性

* 支持多种文件输入方式：文件路径、http（get）下载、ftp，可扩展其他协议。
* 支持多种文件格式。
* 支持水印：文字水印、图片水印、首页表格水印、页码。
* 支持生成双层PDF。
* 支持多种文件回写方式：文件路径（path）、http协议上传（url）、FTP服务上传（ftp），可扩展其他协议。
* 支持转换结果回调。
* 支持失败延迟重试。
* 支持PDF加密，可以为生成的PDF加入密码、使用权限控制。（OFD暂不支持此特性）
* 支持契约锁电子签验证（不可见签章）。

## 依赖

* `jdk8`: 编译、运行环境
* `转换引擎`：
  * `LibreOffice`：支持Windows、Linux环境，但是对Word文档兼容性不好，容易跑版。不推荐。
  * `WPS应用`：Windows版本地应用程序，速度快，版式兼容性好。推荐。
  * `Office应用`：Windows版本地应用程序，速度较慢，单线程。对WPS生成的文档格式兼容性不好，会跑版。且生成PDF无法使用在线的pdf.js预览。谨慎使用。
* `maven`: 编译打包，只运行`jar`不需要，建议`V3.6.3`以上版本
* `rabbitMQ`: 重试机制依赖MQ延迟队列，需安装插件 `rabbitmq_delayed_message_exchange`

## 快速启动

1. 获取`jar`包：联系档案项目组或使用`mvn clean package -Dmaven.test.skip=true`编译,

2. 修改配置`application.yml`：

3. 1. 接收的输入文件存储的临时文件夹:`convert.path.inPutTempPath`:
   
   > Windows： D:/work/input/
   > 
   > Linux： /work/input/
   
   2. 默认的本地输出文件路径: `convert.path.outPutPath`:
   
   > Windows： D:/work/output/
   > 
   > Linux： /work/output/
   
   3. 如需支持失败重试功能，需配置 RabbitMQ 相关参数
      
      > `spring.rabbitmq.host`: RabbitMQ IP地址， 例：10.3.214.12
      > 
      > `spring.rabbitmq.port`: RabbitMQ 端口号, 例： 5672
      > 
      > `spring.rabbitmq.username`: RabbitMQ 用户名, 例： guest
      > 
      > `spring.rabbitmq.password`: RabbitMQ 用户密码, 例： guest
      > 
      > `spring.rabbitmq.listener.direct.auto-startup`: RabbitMQ 生产者 开关, 例： true | false, true: 标识启用功能
      > 
      > `spring.rabbitmq.listener.simple.auto-startup`: RabbitMQ 消费者 开关, 例： true | false, true: 标识启用功能
      > 
      > `convert.retry.max`: 重试次数（0-8），0标识不重试, 若出现异常情况只记录日志， 大于1（最大8）：标识失败重试的次数, 将会在以下时间重试（5min, 10min, 30min, 1h, 2h, 4h, 8h, 16h），例：3, 标识将在5分钟后进行第一次重试，如果还失败，将在10分钟后（即初次转换15分钟后）进行第二次重试. 如果还失败，将在30分钟后（即初次转换45分钟后）进行第三次重试  

4. 确认文件目录结构

```
│  application.yml               配置文件
│  convertpreview-{版本号}.jar    运行jar
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
