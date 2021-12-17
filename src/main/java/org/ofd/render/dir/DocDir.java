package org.ofd.render.dir;

import org.ofdrw.core.annotation.Annotations;
import org.ofdrw.core.annotation.pageannot.PageAnnot;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.pageObj.CT_TemplatePage;
import org.ofdrw.core.basicStructure.res.Res;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class DocDir implements DirCollect {

    /**
     * 表示第几份文档，从0开始
     */
    private Integer index = 0;
    
    /**
     * 文档的根节点
     */
    private Document document;
    
    /**
     * 注释文档
     */
    private Annotations annotations;
    
    /**
     * 注释项文档
     */
    private PageAnnot pageAnnot;
        
    /**
     * 文档公共资源索引
     */
    private Res publicRes;

    /**
     * 文档自身资源索引
     */
    private Res documentRes;

    /**
     * 资源文件夹
     */
    private ResDir res;
    /**
     * 资源文件夹
     */
    private TemplatesDir tpls;

    /**
     * 数字签名存储目录
     */
    private SignsDir signs;

    /**
     * 页面存储目录
     */
    private PagesDir pages;
    
    
    private CT_TemplatePage templateRes;
    
    /**
     * 附件存储目录
     */
    private AttachsDir attachs;
    
    /**
     * 注释目录
     */
    private AnnotsDir annotsDir;
    
    public DocDir() {

    }

    /**
     * @return 文档编号（用于表示第几个） ，从0 起
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * 设值 文档编号（用于表示第几个）
     *
     * @param index 第几个
     * @return this
     */
    public DocDir setIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * @return 文档的根节点
     */
    public Document getDocument() {
        return document;
    }

    /**
     * 设置 文档的根节点
     *
     * @param document 文档的根节点
     * @return this
     */
    public DocDir setDocument(Document document) {
        this.document = document;
        return this;
    }
    
    /**
     * 注释文档
     * @return
     */
    public Annotations getAnnotations() {
    	return annotations;
    }
    
    /**
     * 注释文档
     * @param annotations
     * @return
     */
    public DocDir setAnnotations(Annotations annotations) {
    	this.annotations = annotations;
    	return this;
    }
    
    /**
     * 注释项文档
     * @return
     */
    public PageAnnot getPageAnnot() {
    	return pageAnnot;
    }
    
    /**
     * 注释项文档
     * @param annot
     * @return
     */
    public DocDir setPageAnnot(PageAnnot annot) {
    	this.pageAnnot = annot;
    	return this;
    }

    /**
     * @return 文档公共资源索引
     */
    public Res getPublicRes() {
        return publicRes;
    }

    /**
     * 设置 文档公共资源索引
     *
     * @param publicRes 文档公共资源索引
     * @return this
     */
    public DocDir setPublicRes(Res publicRes) {
        this.publicRes = publicRes;
        return this;
    }

    /**
     * @return 文档自身资源索引
     */
    public Res getDocumentRes() {
        return documentRes;
    }

    /**
     * 设置 文档自身资源索引
     *
     * @param documentRes 文档自身资源索引
     * @return this
     */
    public DocDir setDocumentRes(Res documentRes) {
        this.documentRes = documentRes;
        return this;
    }

    /**
     * @return 资源文件夹
     */
    public ResDir getRes() {
        return res;
    }

    /**
     * 设置 资源文件夹
     *
     * @param res 资源文件夹
     * @return this
     */
    public DocDir setRes(ResDir res) {
        this.res = res;
        return this;
    }

    /**
     * @return 数字签名存储目录
     */
    public SignsDir getSigns() {
        return signs;
    }

    /**
     * 设置 数字签名存储目录
     *
     * @param signs 数字签名存储目录
     * @return this
     */
    public DocDir setSigns(SignsDir signs) {
        this.signs = signs;
        return this;
    }

    /**
     * @return 页面存储目录
     */
    public PagesDir getPages() {
        return pages;
    }

    /**
     * 设置 页面存储目录
     *
     * @param pages 页面存储目录
     * @return this
     */
    public DocDir setPages(PagesDir pages) {
        this.pages = pages;
        return this;
    }

    
    /**
     * @return 页面存储目录
     */
    public TemplatesDir getTemplates() {
        return tpls;
    }

    /**
     * 设置 页面存储目录
     *
     * @param pages 页面存储目录
     * @return this
     */
    public DocDir setPages(TemplatesDir tpls) {
        this.tpls = tpls;
        return this;
    }
    
    /**
     * 增加资源(资源文件路径)
     *
     * @param resource 资源
     * @return this
     */
    public DocDir addResource(Path resource) {
        if (this.res == null) {
            this.res = new ResDir();
        }
        this.res.add(resource);
        return this;
    }
    
    /**
     * 添加资源(资源文件二进制内容)
     * @param name
     * @param resData
     * @return
     */
    public DocDir addResource(String name, byte[] resData) {
        if (res == null) {
            res = new ResDir();
        }  
        
        res.add(name, resData);
        return this;
    }
    
    /**
     * 增加资源
     *
     * @param resource 资源
     * @return this
     */
    public DocDir addResources(List<Path> resources) {
        if (this.res == null) {
            this.res = new ResDir();
        }
        for (Path path : resources) {
        	this.res.add(path);
		}
        return this;
    }
    

    /**
     * 获取资源
     *
     * @param name 资源名称（包含后缀名称）
     * @return 资源，不存在则返还null
     */
    public Path getResource(String name) {
        if (this.res == null) {
            return null;
        }
        return this.res.get(name);
    }
    
    
	public CT_TemplatePage getTemplateRes() {
		return templateRes;
	}

	public DocDir setTemplateRes(CT_TemplatePage templateRes) {
		this.templateRes = templateRes;
		return this;
	}

	public TemplatesDir getTpls() {
		return tpls;
	}

	public DocDir setTpls(TemplatesDir tpls) {
		this.tpls = tpls;
		return this;
	}
	
    /**
     * 附件存储目录
     * @return
     */
    public AttachsDir getAttachs() {
    	return attachs;
    }
    
    /**
     * 附件存储目录
     * @param attachs
     * @return
     */
    public DocDir setAttachs(AttachsDir attachs) {
    	this.attachs = attachs;
    	return this;
    }

	/**
     * 创建目录并复制文件
     *
     * @param base 基础路径
     * @return 创建的目录路径
     * @throws IOException IO异常
     */
    @Override
    public Path collect(String base) throws IOException {
        if (document == null) {
            throw new IllegalArgumentException("文档根节点（document）为空");
        }

        Path path = Paths.get(base, "Doc_" + index);
        path = Files.createDirectories(path);
        String dir = path.toAbsolutePath().toString();

        DocObjDump.dump(document, Paths.get(dir, "Document.xml"));
        
        if (annotations != null) {
        	DocObjDump.dump(annotations, Paths.get(dir, "Annotations.xml"));
        }
        
        if (this.pageAnnot != null) {
        	DocObjDump.dump(pageAnnot, Paths.get(dir, "Annotation.xml"));
        }
        
        if (signs != null) {
            signs.collect(dir);
        }
        if (pages != null) {
            pages.collect(dir);
        }
        if (publicRes != null) {
            DocObjDump.dump(publicRes, Paths.get(dir, "PublicRes.xml"));
        }
        if (documentRes != null) {
            DocObjDump.dump(documentRes, Paths.get(dir, "DocumentRes.xml"));
        }
        if (res != null) {
            res.collect(dir);
        }
        if(tpls !=null) {
        	tpls.collect(dir);
        }
        
        if (attachs != null) {
        	attachs.collect(dir);
        }
        
        if (annotsDir != null) {
        	annotsDir.collect(dir);
        }
        
        return path;
    }

    @Override
    public Map<String, byte[]> collect(String base, Map<String, byte[]> virtualFileMap) throws IOException {
        if (document == null) {
            throw new IllegalArgumentException("文档根节点（document）为空");
        }

        Path path = Paths.get(base, "Doc_" + index);
        String dir = path.toString();

        DocObjDump.dump(document, Paths.get(dir, "Document.xml").toString(), virtualFileMap);

        if (annotations != null) {
            DocObjDump.dump(annotations, Paths.get(dir, "Annotations.xml").toString(), virtualFileMap);
        }

        if (this.pageAnnot != null) {
            DocObjDump.dump(pageAnnot, Paths.get(dir, "Annotation.xml").toString(), virtualFileMap);
        }

        if (signs != null) {
            signs.collect(dir, virtualFileMap);
        }
        if (pages != null) {
            pages.collect(dir, virtualFileMap);
        }
        if (publicRes != null) {
            DocObjDump.dump(publicRes, Paths.get(dir, "PublicRes.xml").toString(), virtualFileMap);
        }
        if (documentRes != null) {
            DocObjDump.dump(documentRes, Paths.get(dir, "DocumentRes.xml").toString(), virtualFileMap);
        }
        if (res != null) {
            res.collect(dir, virtualFileMap);
        }
        if(tpls !=null) {
            tpls.collect(dir, virtualFileMap);
        }

        if (attachs != null) {
            attachs.collect(dir, virtualFileMap);
        }

        if (annotsDir != null) {
            annotsDir.collect(dir, virtualFileMap);
        }

        return virtualFileMap;
    }

    /**
     * 注释目录
     */
	public AnnotsDir getAnnotsDir() {
		return annotsDir;
	}

    /**
     * 注释目录
     */
	public void setAnnotsDir(AnnotsDir annotsDir) {
		this.annotsDir = annotsDir;
	}
    
    
}
