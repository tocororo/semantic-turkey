package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class HeaderWhole {
	private final Map<String, HeaderProp> reifiedPropNameToHeaderPropList = new HashMap<>();
	private final Map<String, HeaderProp> simplePropNameToHeaderPropList = new HashMap<>();

	private final List<String> reifiedPropNameList = new ArrayList<>();
	private final List<String> simplePropNameList = new ArrayList<>();

	private final TreeSet<String>orderedLangList = new TreeSet<>();

	public HeaderWhole() {
	}

	public List<String> getReifiedPropNameList() {
		return reifiedPropNameList;
	}

	public List<String> getSimplePropNameList() {
		return simplePropNameList;
	}

	public HeaderProp getHeaderReifiedPropFromPropName(String propName){
		return reifiedPropNameToHeaderPropList.get(propName);
	}

	public HeaderProp getHeaderSimplePropFromPropName(String propName){
		return simplePropNameToHeaderPropList.get(propName);
	}

	public void updateNumForLangInReifiedProp(String propName, String lang, int num) {
		if(!reifiedPropNameList.contains(propName)){
			reifiedPropNameList.add(propName);
			reifiedPropNameToHeaderPropList.put(propName, new HeaderProp(propName));
		}
		reifiedPropNameToHeaderPropList.get(propName).setNumForLang(lang, num);

		//add the language, if not already present, in the orderedLangList
		if(!orderedLangList.contains(lang)){
			orderedLangList.add(lang);
		}
	}

	public void updateNumForLangInSimpleProp(String propName, String lang, int num) {
		if(!simplePropNameList.contains(propName)){
			simplePropNameList.add(propName);
			simplePropNameToHeaderPropList.put(propName, new HeaderProp(propName));
		}
		simplePropNameToHeaderPropList.get(propName).setNumForLang(lang, num);

		//add the language, if not already present, in the orderedLangList
		if(!orderedLangList.contains(lang)){
			orderedLangList.add(lang);
		}
	}

	/**
	 * return the list of ALL the languages present in the dataset
	 * @return the list of ALL the languages present in the dataset
	 */
	public TreeSet<String> getOrderedLangList() {
		return orderedLangList;
	}
}
