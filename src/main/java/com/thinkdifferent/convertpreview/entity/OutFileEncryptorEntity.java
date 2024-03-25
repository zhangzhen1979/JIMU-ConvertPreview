package com.thinkdifferent.convertpreview.entity;

import lombok.Data;

@Data
public class OutFileEncryptorEntity {

    /**
     * 是否启用加密
     */
    private Boolean encry;
    /**
     * 用户名。PDF、OFD共用。为兼容【超越版式办公套件】，OFD文件建议传入固定值admin
     */
    private String userName;
    /**
     * 用户密码。PDF、OFD共用
     */
    private String userPassWord;
    /**
     * 文档所有者密码。PDF有效。
     */
    private String ownerPassword;

    /**
     * 以下是PDF、OFD共用设置项
     */
    /**
     * 是否可以复制。PDF、OFD均有效。
     */
    private Boolean copy;
    /**
     * 是否可编辑。PDF、OFD均有效。
     */
    private Boolean modify;
    /**
     * 是否可打印。PDF、OFD均有效。
     */
    private Boolean print;
    /**
     * 是否可以添加或修改文本注释并填写交互式表单字段，如果canModify()返回true，则创建或修改交互式表单字段（包括签名字段）。
     */
    private Boolean modifyAnnotations;


    /**
     * 以下为PDF专用设置项
     */
    /**
     * 是否可以插入/删除/旋转页面
     */
    private Boolean assembleDocument;
    /**
     * 是否可以填写交互式表单字段（包括签名字段）
     */
    private Boolean fillInForm;
    /**
     * 是否可以降级格式打印文档
     */
    private Boolean printDegraded;


    /**
     * 以下为OFD专用设置项
     */
    /**
     * 允许打印的份数（允许打印时有效）
     */
    private Integer copies;
    /**
     * 是否允许添加签章
     */
    private Boolean signature;
    /**
     * 是否允许添加水印
     */
    private Boolean watermark;
    /**
     * 是否允许导出
     */
    private Boolean export;
    /**
     * 有效期：开始日期
     */
    private String validPeriodStart;
    /**
     * 有效期：结束日期
     */
    private String validPeriodEnd;

    /**
     * 语言
     */
    private String local;

    /**
     * Aes加密key
     */
    private String aesKey;
    /**
     * 检查文件有效期、用户名密码的接口URL
     */
    private String checkPermissions;
}
