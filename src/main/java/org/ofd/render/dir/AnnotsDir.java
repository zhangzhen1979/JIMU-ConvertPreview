package org.ofd.render.dir;

import org.ofdrw.core.annotation.Annotations;
import org.ofdrw.core.annotation.pageannot.PageAnnot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author 权观宇
 * @time 2020/10/1
 */
public class AnnotsDir implements DirCollect {
    /**
     * 注释文档
     */
    private Annotations annotations;
    
    /**
     * 注释项文档
     */
    private PageAnnot pageAnnot;

	@Override
	public Path collect(String base) throws IOException {
		// TODO Auto-generated method stub
		
        Path path = Paths.get(base, "Annots");
        path = Files.createDirectories(path);
        String dir = path.toAbsolutePath().toString();
        
        if (annotations != null) {
        	DocObjDump.dump(annotations, Paths.get(dir, "Annotations.xml"));
        }
        
        if (this.pageAnnot != null) {
        	DocObjDump.dump(pageAnnot, Paths.get(dir, "Annotation.xml"));
        }
        
		return path;
	}

	@Override
	public Map<String, byte[]> collect(String base, Map<String, byte[]> virtualFileMap) throws IOException {
		Path path = Paths.get(base, "Annots");
		String dir = path.toString();

		if (annotations != null) {
			DocObjDump.dump(annotations, Paths.get(dir, "Annotations.xml").toString(), virtualFileMap);
		}

		if (this.pageAnnot != null) {
			DocObjDump.dump(pageAnnot, Paths.get(dir, "Annotation.xml").toString(), virtualFileMap);
		}

		return virtualFileMap;
	}

	/**
     * 注释文档
     */
	public Annotations getAnnotations() {
		return annotations;
	}

    /**
     * 注释文档
     */
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}

    /**
     * 注释项文档
     */
	public PageAnnot getPageAnnot() {
		return pageAnnot;
	}

    /**
     * 注释项文档
     */
	public void setPageAnnot(PageAnnot pageAnnot) {
		this.pageAnnot = pageAnnot;
	}

}
