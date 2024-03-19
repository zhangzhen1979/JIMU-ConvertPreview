package com.thinkdifferent.convertpreview.engine.impl.onlyOffice.vo.document;

import lombok.Data;

/**
 * @BelongsProject: leaf-onlyoffice
 * @BelongsPackage: com.ideayp.leaf.onlyoffice.dto.convert
 * @Author: TongHui
 * @CreateTime: 2022-11-23 09:46
 * @Description: 文档渲染器
 * @Version: 1.0
 */
@Data
public class DocumentRenderer {
    /***
     * 定义可以具有以下值的呈现模式：
     * 块字符（blockChar） - 所有文本都由单个字符转换。 每个字符都在自己的框架中（如文本框），
     * blockLine- 所有文本都由单独的行转换。 每个文本行都位于其自己的框架中。行可以在同一块内组合，
     * 纯行（plainLine） - 所有文本都转换为纯文本。 但每一行都是一个单独的段落，
     * 纯段落（plainParagraph ） - 所有文本都转换为纯文本。 行合并为段落。
     * 默认值为plainLine。
     */
    private String textAssociation;
}
