package it.uniroma2.art.semanticturkey.properties;

import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;

public class STPropertiesUtils {
	
	/**
	 * Useful to convert the value of the project-setting languages (that is a json representation of objects {name, tag})
	 * into an array of Lanugage objects
	 * @param languagesValue
	 * @return
	 * @throws JSONException
	 */
	public static Collection<Language> parseLanguages(String languagesValue) throws JSONException {
		Collection<Language> languages = new ArrayList<>();
		JSONArray jsonLangsArray = new JSONArray(languagesValue);
		for (int i = 0; i < jsonLangsArray.length(); i++) {
			languages.add(new Language(jsonLangsArray.getJSONObject(i).getString("name"), 
					jsonLangsArray.getJSONObject(i).getString("tag")));
		}
		return languages;
		
	}

}
