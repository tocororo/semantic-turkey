package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures;

import org.eclipse.rdf4j.model.IRI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceInfo {
	private final IRI resourceIRI;
	private final Map<String, PropInfoAndValues> propToValuesForPropMap = new HashMap<>();

	public ResourceInfo(IRI resourceIRI) {
		this.resourceIRI = resourceIRI;
	}

	public IRI getResourceIRI() {
		return resourceIRI;
	}


	@Override
	public boolean equals(Object object){
		boolean same = false;

		if(object instanceof ResourceInfo) {
			same = this.resourceIRI.equals(((ResourceInfo) object).getResourceIRI());
		}

		return same;
	}

	public int addPropWithValuesForProp(String propName, String langTag, ValueForProp valueForProp){
		if(!propToValuesForPropMap.containsKey(propName)){
			propToValuesForPropMap.put(propName, new PropInfoAndValues(propName));
		}
		return propToValuesForPropMap.get(propName).addValue(langTag, valueForProp);
	}

	public PropInfoAndValues getPropInfoAndValues(String prop){
		return propToValuesForPropMap.get(prop);
	}
}
