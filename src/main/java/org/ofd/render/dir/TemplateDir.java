package org.ofd.render.dir;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ofdrw.core.basicStructure.pageObj.Template;
import org.ofdrw.core.basicStructure.res.Res;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


/**
 * 页面目录
 *
 * @author 权观宇
 * @since 2020-01-18 03:05:23
 */
public class TemplateDir implements DirCollect {
    /**
     * 代表OFD中第几页
     * <p>
     * index 从 0 开始取
     */
    private Integer index = 0;

    /**
     * 页面资源
     * <p>
     * 记录了资源的目录
     */
    private Res pageRes;

    /**
     * 资源容器
     */
    private ResDir res;

    /**
     * 页面描述
     */
    private Template content;
    
    /**
     * 页面描述Stream
     */
    private InputStream contentOfStream;
    
    /**
     * 页面描述文件path，若设置了content或contentOfStream的则无效
     */
    private Path contentOfPath;

    public TemplateDir() {
    }

    public TemplateDir(Integer index, Res pageRes, ResDir res, Template content) {
        this.index = index;
        this.pageRes = pageRes;
        this.res = res;
        this.content = content;
    }

    /**
     * @return 页码
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * 设置页码
     *
     * @param index 页码
     * @return this
     */
    public TemplateDir setIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * @return 资源
     */
    public Res getPageRes() {
        return pageRes;
    }

    public TemplateDir setPageRes(Res pageRes) {
        this.pageRes = pageRes;
        return this;
    }

    /**
     * 向页面中增加页面资源
     *
     * @param resource 资源
     * @return this
     */
    public TemplateDir add(Path resource) {
        if (res == null) {
            res = new ResDir();
        }
        this.res.add(resource);
        return this;
    }

    /**
     * 获取页面资源
     *
     * @param name 资源名称，包含后缀
     * @return 资源路径，如果资源不存在则为null
     */
    public Path get(String name) {
        if (this.res == null) {
            return null;
        }
        return this.res.get(name);
    }

    /**
     * @return 获取资源目录
     */
    public ResDir getResDir() {
        return this.res;
    }

    /**
     * @return 页面描述
     */
    public Template getContent() {
        return content;
    }

    /**
     * 设置页面描述
     *
     * @param content 页面描述
     * @return this
     */
    public TemplateDir setContent(Template content) {
        this.content = content;
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
        Path path = Paths.get(base, "Tpl_" + index);
        path = Files.createDirectories(path);
        String dir = path.toAbsolutePath().toString();

        if (content != null) {
            DocObjDump.dump(this.content, Paths.get(dir, "Content.xml"));
        } else if (contentOfStream != null) {
        	Files.copy(contentOfStream, Paths.get(dir, "Content.xml"));
        } else if (contentOfPath != null) {
        	Files.copy(contentOfPath, Paths.get(dir, "Content.xml"));
        }
        
        
        if (res != null) {
            res.collect(dir);
        }
        if (pageRes != null) {
            DocObjDump.dump(this.pageRes, Paths.get(dir, "PageRes.xml"));
        }
        return path;
    }

    @Override
    public Map<String, byte[]> collect(String base, Map<String, byte[]> virtualFileMap) throws IOException {
        Path path = Paths.get(base, "Tpl_" + index);
        String dir = path.toString();

        if (content != null) {
            DocObjDump.dump(this.content, Paths.get(dir, "Content.xml").toString(), virtualFileMap);
        } else if (contentOfStream != null) {
//            Files.copy(contentOfStream, Paths.get(dir, "Content.xml"));
            virtualFileMap.put(Paths.get(dir, "Content.xml").toString(), IOUtils.toByteArray(contentOfStream));
        } else if (contentOfPath != null) {
//            Files.copy(contentOfPath, Paths.get(dir, "Content.xml"));
            virtualFileMap.put(Paths.get(dir, "Content.xml").toString(), FileUtils.readFileToByteArray(contentOfPath.toFile()));
        }


        if (res != null) {
            res.collect(dir);
        }
        if (pageRes != null) {
            DocObjDump.dump(this.pageRes, Paths.get(dir, "PageRes.xml").toString(), virtualFileMap);
        }
        return virtualFileMap;
    }

    /**
     * 页面描述文件path，若设置了content的则无效
     */
	public Path getContentOfPath() {
		return contentOfPath;
	}

    /**
     * 页面描述文件path，若设置了content的则无效
     */
	public void setContentOfPath(Path contentOfPath) {
		this.contentOfPath = contentOfPath;
	}

	public InputStream getContentOfStream() {
		return contentOfStream;
	}

	public void setContentOfStream(InputStream contentOfStream) {
		this.contentOfStream = contentOfStream;
	}
}
