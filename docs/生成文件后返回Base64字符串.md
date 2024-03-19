# 生成文件后返回Base64字符串

生成一个目标文件后返回Base64字符串接口URL：[http://host:port/api/convert2base64](http://host:port/api/convert2base64)

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
