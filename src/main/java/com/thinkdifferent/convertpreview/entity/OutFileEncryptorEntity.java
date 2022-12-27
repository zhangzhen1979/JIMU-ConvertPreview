package com.thinkdifferent.convertpreview.entity;

public class OutFileEncryptorEntity {

    /**
     * 是否启用加密
     */
    private Boolean encry;
    /**
     * 用户名。PDF有效。OFD暂为固定值admin，以兼容【超越版式办公套件】
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


    public String getValidPeriodStart() {
        return validPeriodStart;
    }

    public void setValidPeriodStart(String validPeriodStart) {
        this.validPeriodStart = validPeriodStart;
    }

    public String getValidPeriodEnd() {
        return validPeriodEnd;
    }

    public void setValidPeriodEnd(String validPeriodEnd) {
        this.validPeriodEnd = validPeriodEnd;
    }

    public Boolean getAssembleDocument() {
        return assembleDocument;
    }

    public void setAssembleDocument(Boolean assembleDocument) {
        this.assembleDocument = assembleDocument;
    }

    public Boolean getFillInForm() {
        return fillInForm;
    }

    public void setFillInForm(Boolean fillInForm) {
        this.fillInForm = fillInForm;
    }

    public Boolean getModifyAnnotations() {
        return modifyAnnotations;
    }

    public void setModifyAnnotations(Boolean modifyAnnotations) {
        this.modifyAnnotations = modifyAnnotations;
    }

    public Boolean getPrintDegraded() {
        return printDegraded;
    }

    public void setPrintDegraded(Boolean printDegraded) {
        this.printDegraded = printDegraded;
    }

    public Boolean getSignature() {
        return signature;
    }

    public void setSignature(Boolean signature) {
        this.signature = signature;
    }

    public Boolean getWatermark() {
        return watermark;
    }

    public void setWatermark(Boolean watermark) {
        this.watermark = watermark;
    }

    public Boolean getExport() {
        return export;
    }

    public void setExport(Boolean export) {
        this.export = export;
    }

    public Integer getCopies() {
        return copies;
    }

    public void setCopies(Integer copies) {
        this.copies = copies;
    }

    public Boolean getEncry() {
        return encry;
    }

    public void setEncry(Boolean encry) {
        this.encry = encry;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassWord() {
        return userPassWord;
    }

    public void setUserPassWord(String userPassWord) {
        this.userPassWord = userPassWord;
    }

    public String getOwnerPassword() {
        return ownerPassword;
    }

    public void setOwnerPassword(String ownerPassword) {
        this.ownerPassword = ownerPassword;
    }

    public Boolean getCopy() {
        return copy;
    }

    public void setCopy(Boolean copy) {
        this.copy = copy;
    }

    public Boolean getModify() {
        return modify;
    }

    public void setModify(Boolean modify) {
        this.modify = modify;
    }

    public Boolean getPrint() {
        return print;
    }

    public void setPrint(Boolean print) {
        this.print = print;
    }
}
