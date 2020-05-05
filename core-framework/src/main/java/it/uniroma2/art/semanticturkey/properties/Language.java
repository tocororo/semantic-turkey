package it.uniroma2.art.semanticturkey.properties;

public class Language {
	
	private String name;
	private String tag;
	
	public Language(String name, String tag) {
		super();
		this.name = name;
		this.tag = tag;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getTag() {
		return tag;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}
	
}
