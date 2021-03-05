package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropInfoAndValues {

	public static String NO_LANG_TAG = "_no_lang_tag";

	private final String propName;
	private final Map<String, List<ValueForProp>> langTagToValueListMap = new HashMap<>();

	public PropInfoAndValues(String propName) {
		this.propName = propName;
	}

	public String getPropName() {
		return propName;
	}

	public Collection<String> getLangTagList() {
		return langTagToValueListMap.keySet();
	}

	public List<ValueForProp> getValueForPropListFromLang(String lang){
		return langTagToValueListMap.get(lang);
	}

	/**
	 * It add a value for the prop, if not already present and it return the number of different values for the specified langTag
	 * @param lang the lang of the valueForProp (use PropInfoAndValues.NO_LANG_TAG if the value has no langauge tag)
	 * @param valueForProp to value to be added for the desired lang
	 * @return the number of values associated to selected lang
	 */
	public int addValue(String lang, ValueForProp valueForProp){
		if(!langTagToValueListMap.containsKey(lang)){
			langTagToValueListMap.put(lang, new ArrayList<>());
		}
		List<ValueForProp> valueForPropList = langTagToValueListMap.get(lang);
		if(!valueForPropList.contains(valueForProp)){
			valueForPropList.add(valueForProp);
		}
		return valueForPropList.size();
	}
}
