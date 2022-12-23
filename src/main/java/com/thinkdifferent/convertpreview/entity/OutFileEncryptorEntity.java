package com.thinkdifferent.convertpreview.entity;

public class OutFileEncryptorEntity {

    private Boolean encry;
    private String userName;
    private String userPassWord;
    private String ownerPassword;
    private Boolean copy;
    private Boolean modify;
    private Boolean print;

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
