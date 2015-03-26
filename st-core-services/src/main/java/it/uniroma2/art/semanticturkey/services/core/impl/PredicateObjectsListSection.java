package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;

import org.w3c.dom.Element;

public class PredicateObjectsListSection implements ResourceViewSection {

	private PredicateObjectsList predicateObjectsList;

	public PredicateObjectsListSection(PredicateObjectsList predicateObjectsList) {
		this.predicateObjectsList = predicateObjectsList;
	}

	@Override
	public void appendToElement(Element parent) {
		RDFXMLHelp.addPredicateObjectList(parent, predicateObjectsList);
	}
}