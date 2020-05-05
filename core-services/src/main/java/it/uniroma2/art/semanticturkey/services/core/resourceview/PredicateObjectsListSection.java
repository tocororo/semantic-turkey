package it.uniroma2.art.semanticturkey.services.core.resourceview;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using=PredicateObjectsListSectionSerializer.class)
public class PredicateObjectsListSection implements ResourceViewSection {
	private PredicateObjectsList predicateObjectsList;

	public PredicateObjectsListSection(PredicateObjectsList predicateObjectsList) {
		this.predicateObjectsList = predicateObjectsList;
	}
	
	public PredicateObjectsList getPredicateObjectsList() {
		return predicateObjectsList;
	}
	
}
