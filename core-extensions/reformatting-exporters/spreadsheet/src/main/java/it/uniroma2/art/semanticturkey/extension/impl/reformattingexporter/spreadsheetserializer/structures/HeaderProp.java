package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures;

import java.util.HashMap;
import java.util.Map;

public class HeaderProp {
	private String propName;
	private Map<String, Integer> langToCountMap = new HashMap<>();

	public HeaderProp(String propName) {
		this.propName = propName;
	}

	public String getPropName() {
		return propName;
	}

	/**
	 * Update the number for the specified lang with the specified num, if the specified num is bigger than the
	 * previous value
	 * @param lang
	 * @param num
	 */
	public void setNumForLang(String lang, int num) {
		if(!langToCountMap.containsKey(lang)){
			langToCountMap.put(lang, num);
		} else if(langToCountMap.get(lang)<num){
			langToCountMap.put(lang, num);
		}
	}

	public int getNumForLang(String lang){
		return langToCountMap.getOrDefault(lang, 0);
	}
}
