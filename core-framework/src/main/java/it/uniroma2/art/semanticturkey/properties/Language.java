package it.uniroma2.art.semanticturkey.properties;

public class Language implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.properties.Language";

		public static final String shortName = keyBase + ".shortName";
		public static final String name$description = keyBase
				+ ".name.description";
		public static final String name$displayName = keyBase
				+ ".name.displayName";
		public static final String tag$description = keyBase
				+ ".tag.description";
		public static final String tag$displayName = keyBase
				+ ".tag.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" +MessageKeys.name$description + "}", displayName = "{" + MessageKeys.name$displayName + "}")
	public String name;

	@STProperty(description = "{" +MessageKeys.tag$description + "}", displayName = "{" + MessageKeys.tag$displayName + "}")
	public String tag;

	public Language() {
	}

	public Language(String name, String tag) {
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
