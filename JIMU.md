# JIMU
JIMU Platform 积木框架，解决微服务支撑问题，为“单体架构”向“微服务架构”演进提供支持。


目前包含如下组件

- DBAgent  解决数据库连接、访问，执行SQL问题。各个微服务可以通过Dubbo接口调用此服务，以此服务为“数据库代理”，执行SQL。此服务支持多实例管理。
- MongoService  解决MongoDB数据库的存储、查询问题。基于Kafka MQ做队列缓冲，减轻MongoDB的写入连接压力。
- LogService   基于MongoDB数据库存储，使用Kafka MQ缓冲，解决平台中各类日志的存储、查询支持。解决集群环境下日志的统一管理问题。
- JIMU-ConvertVideo：将常见视频、音频文件转换为可在线播放的MP4格式。需要FFMpeg支持。
- JIMU-ConvertPic：将TIff、BMP等浏览器不便直接显示的图片文件转换为jpg、pdf。转换过程中可以加入图片水印、文字水印。转换为pdf后可以完美解决纸张打印自适应问题。
- JIMU-ConvertOffice：将常见办公文档格式文件，转换为PDF、OFD，支持添加图片水印、文字水印。
- JIMU-JSON2PDF：将输入的JSON格式的数据，使用报表工具生成PDF文件。



