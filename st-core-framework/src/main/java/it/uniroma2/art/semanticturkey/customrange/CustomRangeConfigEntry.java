package it.uniroma2.art.semanticturkey.customrange;

public class CustomRangeConfigEntry {

	private String property;
	private CustomRange cutomRange;
	private boolean replaceRange;
	
	public CustomRangeConfigEntry(String property, CustomRange cutomRange, boolean replaceRange) {
		this.property = property;
		this.cutomRange = cutomRange;
		this.replaceRange = replaceRange;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public CustomRange getCutomRange() {
		return cutomRange;
	}

	public void setCutomRange(CustomRange cutomRange) {
		this.cutomRange = cutomRange;
	}

	public boolean getReplaceRange() {
		return replaceRange;
	}

	public void setReplaceRange(boolean replaceRange) {
		this.replaceRange = replaceRange;
	}
	
	
}
