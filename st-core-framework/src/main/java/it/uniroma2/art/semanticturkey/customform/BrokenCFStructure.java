package it.uniroma2.art.semanticturkey.customform;

import java.io.File;

public class BrokenCFStructure {
	
	private String id;
	private String type; //class name: CustomFrom, FormCollection or FormsMapping
	private CustomFormLevel level;
	private File file;
	private String reason;
	
	public BrokenCFStructure(String id, String type, CustomFormLevel level, File file, String reason) {
		this.id = id;
		this.type = type;
		this.level = level;
		this.file = file;
		this.reason = reason;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public CustomFormLevel getLevel() {
		return level;
	}
	public void setLevel(CustomFormLevel level) {
		this.level = level;
	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}

	
}
