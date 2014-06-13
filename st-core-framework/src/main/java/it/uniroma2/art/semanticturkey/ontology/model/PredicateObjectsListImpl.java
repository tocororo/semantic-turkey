package it.uniroma2.art.semanticturkey.ontology.model;

import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Multimap;

public class PredicateObjectsListImpl implements PredicateObjectsList {

	Multimap<ARTURIResource, STRDFNode> valueMultiMap;
	Map<ARTURIResource, STRDFResource> propMap;

	PredicateObjectsListImpl(Map<ARTURIResource, STRDFResource> propMap,
			Multimap<ARTURIResource, STRDFNode> valueMultiMap) {
		this.propMap = propMap;
		this.valueMultiMap = valueMultiMap;
	}

	public Collection<STRDFResource> getPredicates() {
		return propMap.values();
	}

	public Collection<STRDFNode> getValues(ARTURIResource predicate) {
		return valueMultiMap.get(predicate);
	}

	public Collection<STRDFNode> getValues(STRDFResource predicate) {
		return valueMultiMap.get((ARTURIResource)predicate.getARTNode());
	}


}
