package it.uniroma2.art.semanticturkey.services.core.resourceview;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using=PredicateValueListSectionSerializer.class)
public class PredicateValueListSection<T> implements ResourceViewSection {
	private PredicateValueList<T> predicateValueList;

	public PredicateValueListSection(PredicateValueList<T> predicateValueList) {
		this.predicateValueList = predicateValueList;
	}
	
	public PredicateValueList<T> getPredicateValueList() {
		return predicateValueList;
	}
}
