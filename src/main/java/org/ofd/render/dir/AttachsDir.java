package org.ofd.render.dir;

import org.apache.commons.io.FileUtils;
import org.dom4j.Element;
import org.ofdrw.core.attachment.Attachments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 权观宇
 * @time 2020/10/1
 */
public class AttachsDir implements DirCollect {
	
	/**
	 * 附件描述文件Attachments.xml
	 */
	private Attachments attachments;
	
	/**
	 * xml格式附件文件
	 */
	private Map<String, Element> attachXmlMap;
	
	/**
	 * path指定的附件文件
	 */
	private Map<String, Path> attachPathMap;
	
	/**
	 * 二进制格式的附件文件
	 */
	private Map<String, byte[]> attachDataMap;
	
	
	public AttachsDir() {
		attachDataMap = new HashMap<>();
		attachPathMap = new HashMap<>();
		attachXmlMap = new HashMap<>();
	}
	
	/**
	 * 附件描述文件Attachments.xml
	 * @return
	 */
	public Attachments getAttachments() {
		return this.attachments;
	}
	
	/**
	 * 附件描述文件Attachments.xml
	 * @param attachments
	 * @return
	 */
	public AttachsDir setAttachments(Attachments attachments) {
		this.attachments = attachments;
		return this;
	}
	
	/**
	 * 添加xml格式的附件
	 * @param name            附件名
	 * @param attach          xml格式的附件
	 * @return
	 * @throws Exception 
	 */
	public AttachsDir addAttachment(String name, Element attach) {	
		this.attachXmlMap.put(name, attach);
		return this;
	}
	
	/**
	 * 添加二进制格式的附件
	 * @param name           附件名称
	 * @param attach         附件二进制内容
	 * @return
	 * @throws Exception 
	 */
	public AttachsDir addAttachment(String name, byte[] attach) {	
		attachDataMap.put(name, attach);
		return this;
	}
	
	/**
	 * 添加Path路径指定的附件
	 * @param name
	 * @param attach
	 * @return
	 * @throws Exception 
	 */
	public AttachsDir addAttachment(String name, Path attach) {		
		attachPathMap.put(name, attach);
		return this;
	}
	
	@Override
	public Path collect(String base) throws IOException {
        if (attachments == null) {
            throw new IllegalArgumentException("文档根节点（Attachments.xml）为空");
        }
        
        Path path = Paths.get(base, "Attachs");
        path = Files.createDirectories(path);
        String dir = path.toAbsolutePath().toString();  
        
        //1. 添加Attachments.xml
        DocObjDump.dump(attachments, Paths.get(dir, "Attachments.xml"));
        
        //2. 添加附件
        //2.1. xml附件
        if (attachXmlMap != null) {
        	for (Map.Entry<String, Element> entry : attachXmlMap.entrySet()) {
        		//System.out.println(entry.getValue().asXML());
        		DocObjDump.dump(entry.getValue(), Paths.get(dir, entry.getKey()));
        	}
        }
        
        //2.2 path指定的附件文件
        if (attachPathMap != null) {
        	for (Map.Entry<String, Path> entry : attachPathMap.entrySet()) {
        		Files.copy(entry.getValue(), Paths.get(dir, entry.getKey()));
        	}
        }
        
        //2.3 二进制文件
        if (attachDataMap != null) {
        	for (Map.Entry<String, byte[]> entry : attachDataMap.entrySet()) {
        		Files.write(Paths.get(dir, entry.getKey()), entry.getValue());
        	}
        }
        
		return path;
	}

	@Override
	public Map<String, byte[]> collect(String base, Map<String, byte[]> virtualFileMap) throws IOException {
		if (attachments == null) {
			throw new IllegalArgumentException("文档根节点（Attachments.xml）为空");
		}

		Path path = Paths.get(base, "Attachs");
		String dir = path.toString();

		//1. 添加Attachments.xml
		DocObjDump.dump(attachments, Paths.get(dir, "Attachments.xml").toString(), virtualFileMap);

		//2. 添加附件
		//2.1. xml附件
		if (attachXmlMap != null) {
			for (Map.Entry<String, Element> entry : attachXmlMap.entrySet()) {
				//System.out.println(entry.getValue().asXML());
				DocObjDump.dump(entry.getValue(), Paths.get(dir, entry.getKey()).toString(), virtualFileMap);
			}
		}

		//2.2 path指定的附件文件
		if (attachPathMap != null) {
			for (Map.Entry<String, Path> entry : attachPathMap.entrySet()) {
//				Files.copy(entry.getValue(), Paths.get(dir, entry.getKey()));
				virtualFileMap.put(Paths.get(dir, entry.getKey()).toString(), FileUtils.readFileToByteArray(new File(entry.getKey())));
			}
		}

		//2.3 二进制文件
		if (attachDataMap != null) {
			for (Map.Entry<String, byte[]> entry : attachDataMap.entrySet()) {
//				Files.write(Paths.get(dir, entry.getKey()), entry.getValue());
				virtualFileMap.put(Paths.get(dir, entry.getKey()).toString(), entry.getValue());
			}
		}

		return virtualFileMap;
	}

}
