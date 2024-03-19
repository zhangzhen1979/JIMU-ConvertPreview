# PDF脱敏（涂抹）说明

请求地址：/api/onlinePreview

请求方式：GET

请求示例：

```url
http://127.0.0.1:8080/api/onlinePreview?filePath=QzpcSmF2YVxjdnRlc3RcMDAxLnBkZg==&fileType=pdf&outType=pdf&md5=abc123def456&writeBackType=path&writeBack=773dc4bc91caf23d08500098533458a7d163a56460410e5122e5c8f90b52641b&writeBackHeaders=&callBackUrl=d02c314ecf65c8fb793b622bae22158be89971d73dc2b2a6b63ee9c84ec43821b61290653b2369f5081ebd425e7f21a452e2f15c5c4b1eafd302ef9e39c696cc
```

格式化后：

```url
http://127.0.0.1:8080/api/onlinePreview?
filePath=QzpcSmF2YVxjdnRlc3RcMDAxLnBkZg==
&fileType=pdf
&outType=pdf
&md5=abc123def456
&writeBackType=path
&writeBack=773dc4bc91caf23d08500098533458a7d163a56460410e5122e5c8f90b52641b
&writeBackHeaders=
&callBackUrl=d02c314ecf65c8fb793b622bae22158be89971d73dc2b2a6b63ee9c84ec43821b61290653b2369f5081ebd425e7f21a452e2f15c5c4b1eafd302ef9e39c696cc
```

请求参数：

- filePath: 【必填】。base64格式后的文件路径。支持以下方式传入文件路径
  
  - 本地文件：C:/a.doc
  - http链接：[http://ip:port/download/fileId](http://ip:port/download/fileId)
    - md5: 非必须，用于区分文件是否是同一个，优先级较高
    - uuid: 非必须，用于区分文件是否是同一个，优先级较低
  - ftp路径：[ftp://username:password@ip:port/dir/file.doc](ftp://username:password@ip:port/dir/file.doc)

- fileType：【必填】。文件类型，即文件的扩展名。

- outType：【必填】。预览模式，本例固定为“pdf”。

- md5：【必填】。原始文件的md5值。如果不填写此项，则会使用uuid命名文件。
  
  ```
  md5=abc123def456
  ```

- writeBackType：【必填】。脱敏后PDF文件回写方式。支持文件路径（path）、服务器端路径（local）、http协议上传（url）、FTP服务上传（ftp）、Ecology接口回写（ecology）。
  
  ```url
  writeBackType=path
  ```

- writeBack：【必填】。【Base64（AES加密）后，URLEncode编码】后的回写地址或URL。（示例为Base64编码之前的值）
  
  ```url
  writeBack={"path":"c:/temp/"}
  ```

- writeBackHeaders：非必填（但需要拼装此参数）。【Base64（AES加密）后，URLEncode编码】后的请求头信息。（示例为Base64编码之前的值）
  
  ```url
  writeBackHeaders={"token":"da3efcbf-0845-4fe3-8aba-ee040be542c0"}
  ```

- callBackUrl：【必填】。【Base64（AES加密）后，URLEncode编码】后的回调接口url。（示例为Base64编码之前的值）
  
  ```url
  callBackURL=http://127.0.0.1/callback.do?file=123.pdf&flag=success
  ```

- controlParams：非必填。【Base64（AES加密）后，URLEncode编码】页面按钮控制权限信息。JSON对象，内容如下：
  
  - PPTMode：演示模式。true/false。
  - print：在线打印。true/false。
  - download：下载到本地。true/false。
  
  ```json
  {
      "PPTMode": true,
      "print": true,
      "download": true
  }
  ```

- keyword：非必填。检索词。如果使用pdf方式预览，可以支持单个词高亮显示。



请求结果：html页面，需通过浏览器进行预览。
