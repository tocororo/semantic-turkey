package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures;

import org.eclipse.rdf4j.model.IRI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConceptInfo extends ResourceInfo {
	private final List<ConceptInfo> narrowerList = new ArrayList<>();
	//add the info about the other properties
	//private final Map<String, PropInfoAndValues> propToValuesForPropMap = new HashMap<>();


	public ConceptInfo(IRI conceptIRI) {
		super(conceptIRI);
	}

	public List<ConceptInfo> getNarrowerList() {
		return narrowerList;
	}

	public boolean addNarrower(ConceptInfo narrower){
		if(narrowerList.contains(narrower)){
			return false;
		}
		narrowerList.add(narrower);
		return true;
	}

	/*
	public int addPropWithValuesForProp(String propName, String langTag, ValueForProp valueForProp){
		if(!propToValuesForPropMap.containsKey(propName)){
			propToValuesForPropMap.put(propName, new PropInfoAndValues(propName));
		}
		return propToValuesForPropMap.get(propName).addValue(langTag, valueForProp);
	}

	public PropInfoAndValues getPropInfoAndValues(String prop){
		return propToValuesForPropMap.get(prop);
	}
	 */


}
